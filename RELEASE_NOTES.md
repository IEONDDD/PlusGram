# Plusgram 4.0.0

- Removed the Creator/Supporter badge system, badge settings, remote user registry and Stars badge bot.
- Replaced the Python Bypass with the complete SoundGram-derived native Go engine, JNI bridge, settings screen, background service and automatic network detection.
- Added upstream Cloudflare routes, FakeTLS, DNS-over-HTTPS, connection pooling, network-change recovery, startup watchdog and service restoration. The engine is built from source.
- Applied Material You defaults across the app: dynamic system colors on Android 12+, 28 dp settings cards, Material 3 switches/sliders, chat headers, navigation and media players.
- Added settings-screen entrance animations, additional press interactions, menu-button feedback, predictive back and motion-speed support for bottom sheets. System animation preferences are respected.
- Fixed Android version-code progression so this release can update previous Plusgram installations.
- Kept private signing material and local configuration out of the release source tree. Removed the former badge-owner/supporter data.

Validation: signed release build succeeded; 156 Python tests passed; Android-native engine tests passed, including a real Telegram MTProto exchange through Cloudflare. Further application/UI testing was stopped at the owner's request.

Requires Android 7.0+ and an arm64-v8a device. Based on [SoundGram](https://github.com/soundgram-project/SoundGram/tree/b489ce80fdc5390844902582d50b0476a6278d78), NagramX, exteraless and Telegram Android; GPL-2.0 licensing and attribution are preserved.
