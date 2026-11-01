# Plusgram 4.0.0 — 2026-11-01

- Signed arm64-v8a release APK: Gradle assembleRelease succeeded.
- Python suite: 156 passed, 34 skipped, one Windows symlink test excluded because Windows does not grant symlink creation privileges.
- Android-native Go tests passed: occupied-port failure, invalid-handshake rejection, shutdown/port reuse and bundled offline Cloudflare domains.
- Live Android emulator test completed a real MTProto req_pq_multi/resPQ exchange through Cloudflare, with matching nonce and a verified Cloudflare connection counter. No Telegram account or user session was used.
- The Go engine matches SoundGram commit b489ce80fdc5390844902582d50b0476a6278d78 byte for byte.
- Source privacy scan: 27,886 publishable files checked, zero findings. 265 public cryptographic test fixtures were classified separately.
- Private signing material and local build configuration are ignored by Git. The public certificate uses the Plusgram name.
- Existing dirty submodule working copies were preserved. Windows runtime build support is tracked in Tools/build_chaquopy_runtime.sh; parent Gradle configuration handles missing Media3 consumer rules.

Additional application/UI verification was stopped at the owner's request. The complete APK has not been smoke-tested. Proxy behavior depends on external route availability and individual networks.

The source privacy scan does not cover historical commits or older releases. Telegram/Firebase client application configuration is expected in Android clients; private signing keys, local build properties and account sessions remain outside release inputs.
