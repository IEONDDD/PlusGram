# Plusgram 4.0.1 — 2026-11-02

- Signed arm64-v8a release built successfully; APK signature verified.
- 20 engine adapter tests passed with actual networking dependencies.
- A live MTProto request returned resPQ with a matching nonce through the original Flowseal engine.
- On the Android emulator, the signed release opened General settings with cards visible without any scrolling.
- The original engine's Android foreground service started successfully, initialized Chaquopy and opened its local listener.
- 27,900 publishable source files and 14,550 APK/nested archive entries scanned: zero privacy findings. 265 public crypto test fixtures were classified separately.
- Retired libtgwsproxy Go/JNI libraries are absent from the APK.

The upstream networking files match Flowseal/tg-ws-proxy commit 2085861957070eeea07990a522b29f42e5230210 unchanged. Android lifecycle and JCA AES selection remain outside upstream files. Operation on every Russian provider has not been verified and cannot be guaranteed.
