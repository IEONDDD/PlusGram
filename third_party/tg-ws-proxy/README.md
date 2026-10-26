# TG WS Proxy

Source: https://github.com/Flowseal/tg-ws-proxy
Pinned commit: `307a384ab7f522e34bb1a045884d0bd90f40bee5`
License: MIT, reproduced in `TMessagesProj/src/main/assets/licenses/tg-ws-proxy.txt`.

The Python core is vendored in `TMessagesProj/src/main/python/plusgram_tgws/vendor`.
Plusgram supplies an Android lifecycle wrapper, loopback-only configuration and a JCA AES-CTR adapter.
The desktop command-line runner and its secret-printing logging are not used.
Cloudflare is opt-in. Auto domains use the upstream project's public relay list; a custom domain overrides it.
