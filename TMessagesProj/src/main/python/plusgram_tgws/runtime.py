"""Android lifecycle adapter. Only loopback is exposed; no CLI or secret logging."""
import asyncio
import ipaddress
import json
import re
import threading

_lock = threading.RLock()
_thread = None
_loop = None
_stop = None
_cancel = False
_state = "off"
_error = ""


def validate(raw):
    data = json.loads(raw) if isinstance(raw, str) else dict(raw)
    for key, default, low, high in (("port", 1443, 1024, 65535),
                                   ("pool", 4, 1, 16), ("buffer", 256, 64, 1024)):
        value = data.get(key, default)
        if type(value) is not int or not low <= value <= high:
            raise ValueError(key)
        data[key] = value
    if not re.fullmatch(r"[0-9a-fA-F]{32}", data.get("secret", "")):
        raise ValueError("secret")
    for key in ("domain", "sni"):
        value = data.get(key, "").strip().lower()
        if value and (len(value) > 253 or not all(
                re.fullmatch(r"[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?", label)
                for label in value.split(".")) or "." not in value):
            raise ValueError(key)
        data[key] = value
    if data.get("fake_tls") and not data["sni"]:
        raise ValueError("sni")
    overrides = {}
    for item in re.split(r"[,;\s]+", data.get("dc", "").strip()):
        if not item:
            continue
        try:
            dc, addr = item.split(":")
            dc = int(dc)
            if dc not in (1, 2, 3, 4, 5, 203):
                raise ValueError()
            overrides[dc] = str(ipaddress.IPv4Address(addr))
        except ValueError:
            raise ValueError("dc") from None
    data["redirects"] = overrides
    return data


def status():
    with _lock:
        return json.dumps({"state": _state, "error": _error})


def start(raw):
    global _thread, _state, _error, _cancel
    data = validate(raw)
    with _lock:
        if _thread is not None and _thread.is_alive():
            raise RuntimeError("TGWS is already running")
        _cancel = False
        _state, _error = "starting", ""
        _thread = threading.Thread(target=_run, args=(data,), name="Plusgram-TGWS", daemon=True)
        _thread.start()


def stop():
    global _cancel
    with _lock:
        _cancel = True
        if _loop is not None and _stop is not None:
            try:
                _loop.call_soon_threadsafe(_stop.set)
            except RuntimeError:
                pass
        thread = _thread
    if thread and thread is not threading.current_thread():
        thread.join(10)
        if thread.is_alive():
            raise RuntimeError("TGWS shutdown timed out")


def _run(data):
    global _state, _error, _loop, _stop
    try:
        asyncio.run(_serve(data))
    except Exception as exc:
        # Never include configuration, URLs, or secret-bearing exception messages.
        with _lock:
            _state, _error = "error", type(exc).__name__
    finally:
        with _lock:
            _loop = _stop = None
            if _state != "error":
                _state = "off"


async def _serve(data):
    global _loop, _stop, _state
    from .vendor import config, tg_ws_proxy as core
    from .vendor.balancer import balancer
    cfg = config.proxy_config
    cfg.host, cfg.port, cfg.secret = "127.0.0.1", data["port"], data["secret"]
    cfg.pool_size, cfg.buffer_size = data["pool"], data["buffer"] * 1024
    cfg.fallback_cfproxy = bool(data.get("cloudflare", False))
    cfg.cfproxy_user_domains = [data["domain"]] if data["domain"] else []
    cfg.cfproxy_worker_domains = []
    cfg.dc_redirects = data["redirects"]
    cfg.fake_tls_domain = data["sni"] if data.get("fake_tls") else ""
    cfg.disable_secure = cfg.proxy_protocol = cfg.force_test_dc = False
    core.reset_tcp_backoff()
    core.ws_pool.reset()
    core.cf_worker_pool.reset()
    core.cf_h2_pool = core.CfH2Pool() if cfg.h2_enabled else None
    clients = set()
    server = None
    with _lock:
        _loop, _stop = asyncio.get_running_loop(), asyncio.Event()
        if _cancel:
            _stop.set()
    def accept(reader, writer):
        if _stop.is_set():
            writer.close()
            return
        task = asyncio.create_task(core._handle_client(reader, writer, bytes.fromhex(cfg.secret)))
        clients.add(task)
        task.add_done_callback(clients.discard)
    try:
        if _stop.is_set():
            return
        server = await asyncio.start_server(accept, cfg.host, cfg.port)
        if cfg.fallback_cfproxy:
            if cfg.cfproxy_user_domains:
                balancer.update_domains_list(cfg.cfproxy_user_domains)
            else:
                config.start_cfproxy_domain_refresh()
        with _lock:
            _state = "running"
        await core.ws_pool.warmup()
        await core.cf_worker_pool.warmup()
        await _stop.wait()
    finally:
        if server:
            server.close()
            await server.wait_closed()
        for task in list(clients):
            task.cancel()
        await asyncio.gather(*clients, return_exceptions=True)
        await core.ws_pool.close()
        await core.cf_worker_pool.close()
        if core.cf_h2_pool is not None:
            await core.cf_h2_pool.close()
            core.cf_h2_pool = None
        config._refresh_stop.set()
