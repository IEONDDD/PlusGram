"""TGWS adapter regression tests: validation, real sockets and shutdown."""
import importlib.util
import json
import os
from pathlib import Path
import socket
import sys
import time

import pytest

ROOT = Path(__file__).resolve().parents[1]
PYTHON = ROOT / "TMessagesProj/src/main/python"
sys.path.insert(0, str(PYTHON))
from plusgram_tgws import runtime


def config(**changes):
    return dict(secret="0123456789abcdef" * 2, **changes)


@pytest.mark.parametrize("change", [
    {"port": 0}, {"port": 65536}, {"pool": 0}, {"pool": 17}, {"buffer": 63},
    {"buffer": True}, {"secret": "bad"}, {"domain": "https://host.test"},
    {"domain": "bad..test"}, {"sni": "-host.test"}, {"fake_tls": True},
    {"dc": "9:1.2.3.4"}, {"dc": "2:300.1.2.3"}, {"dc": "2:localhost"},
])
def test_invalid_config(change):
    data = config()
    data.update(change)
    with pytest.raises(ValueError):
        runtime.validate(data)


def test_valid_config():
    result = runtime.validate(config(dc="2:149.154.167.220; 4:149.154.167.220",
                                     fake_tls=True, sni="Google.com", domain="relay.example.com"))
    assert result["sni"] == "google.com"
    assert result["redirects"] == {2: "149.154.167.220", 4: "149.154.167.220"}
    assert result["port"] == 1443
    assert result["buffer"] == 256


@pytest.fixture
def engine():
    pytest.importorskip("cryptography")
    pytest.importorskip("httpx")
    pytest.importorskip("h2")
    yield runtime
    runtime.stop()


def free_port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def wait_state(engine, wanted):
    deadline = time.monotonic() + 5
    while time.monotonic() < deadline:
        state = json.loads(engine.status())
        if state["state"] == wanted:
            return state
        if state["state"] == "error" and wanted != "error":
            pytest.fail(str(state))
        time.sleep(.01)
    pytest.fail(engine.status())


def test_lifecycle_and_loopback(engine):
    port = free_port()
    engine.start(config(port=port))
    wait_state(engine, "running")
    with socket.create_connection(("127.0.0.1", port), timeout=2) as client:
        client.sendall(bytes(64))  # Not an authenticated MTProto handshake.
        # Upstream deliberately drains unauthenticated clients until EOF.
        client.shutdown(socket.SHUT_WR)
        try:
            assert client.recv(1) == b""
        except ConnectionResetError:
            pass
    with pytest.raises(RuntimeError):
        engine.start(config(port=port))
    engine.stop()
    assert json.loads(engine.status())["state"] == "off"
    # Stop closed the listener; the exact port can be reused immediately.
    engine.start(config(port=port))
    wait_state(engine, "running")


def test_port_collision_does_not_claim_running(engine):
    with socket.socket() as occupied:
        occupied.bind(("127.0.0.1", 0))
        occupied.listen()
        engine.start(config(port=occupied.getsockname()[1]))
        result = wait_state(engine, "error")
        assert result["error"] == "OSError"
        assert config()["secret"] not in engine.status()


def test_immediate_stop_cancels_startup(engine):
    for _ in range(5):
        engine.start(config(port=free_port()))
        engine.stop()
        assert json.loads(engine.status())["state"] == "off"


def test_cloudflare_off_makes_no_domain_refresh(engine, monkeypatch):
    from plusgram_tgws.vendor import config as cfg
    def unexpected():
        pytest.fail("Cloudflare discovery while disabled")
    monkeypatch.setattr(cfg, "start_cfproxy_domain_refresh", unexpected)
    engine.start(config(port=free_port(), cloudflare=False))
    wait_state(engine, "running")


def test_mtproto_secret_authentication(engine):
    import hashlib
    import struct
    from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
    from plusgram_tgws.vendor import tg_ws_proxy as core
    from plusgram_tgws.vendor.utils import PROTO_TAG_SECURE
    secret = bytes.fromhex(config()["secret"])
    header = bytearray(os.urandom(64))
    header[56:60] = PROTO_TAG_SECURE
    header[60:62] = struct.pack("<h", 2)
    key = hashlib.sha256(bytes(header[8:40]) + secret).digest()
    crypt = Cipher(algorithms.AES(key), modes.CTR(bytes(header[40:56]))).encryptor()
    encrypted = crypt.update(bytes(header))
    header[56:] = encrypted[56:]
    parsed = core._try_handshake(bytes(header), secret)
    assert parsed[:3] == (2, False, PROTO_TAG_SECURE)
    assert core._try_handshake(bytes(header), bytes(16)) is None
