# Plusgram 4.0.1

- Fixed Material You settings cards remaining invisible until scrolling. Section backgrounds now redraw throughout row entrance animations.
- Removed the SoundGram Bypass implementation, Go engine and native JNI bridge.
- Integrated the original Flowseal/tg-ws-proxy networking modules unchanged, pinned to commit 2085861957070eeea07990a522b29f42e5230210.
- Enabled upstream Cloudflare fallback by default, including bundled domains and automatic domain refresh. Android service lifecycle and AES/JCA are provided by a separate integration layer.
- Verified a real Telegram MTProto req_pq_multi/resPQ exchange through the original engine; 20 engine adapter tests passed.

Connectivity depends on provider restrictions and route availability; operation on every Russian network is not guaranteed. Android 7.0+, arm64-v8a. Upstream MIT license is included.
