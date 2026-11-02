# Original TG WS Proxy

Source: https://github.com/Flowseal/tg-ws-proxy
Commit: `2085861957070eeea07990a522b29f42e5230210`

All proxy/*.py files are vendored byte-for-byte in plusgram_tgws/vendor.
Android lifecycle and JCA AES selection are implemented outside upstream files.
Cloudflare fallback is enabled by default; the upstream list refresh and bundled domains support cold starts. No route can guarantee connectivity on every provider.
