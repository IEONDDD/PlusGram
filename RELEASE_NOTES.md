# Plusgram 5.3.6

- Softened the edges of Rain and Thunderstorm so clouds, raindrops, and lightning blend into the chat list header in light and dark themes.
- Replaced the default Plusgram icon with the burgundy paper plane and plus mark across the Android launcher, icon picker, in-app branding, and README. Updated the monochrome notification and themed icons to match.
- Fixed a crash when opening Chat Settings before the chat preview resources were loaded.
- Fixed a background timer that could keep waking the built-in notification service after switching to FCM. The built-in service also skips the immediate duplicate wakeup when it starts.

## Plusgram 5.3.5

- Redesigned Northern Lights with softly layered, slowly drifting curtains.
- Reworked Rain and Thunderstorm with wind-driven streaks at different depths, cloud haze, and occasional branching lightning.
- Preserved title readability and a still version of each effect when animations are disabled.
- Fixed the selected header effect resetting when the app restarts.

## Plusgram 5.3.4

- Added an optional Expressive Material You style with lilac accents, rounded cards, and a restyled player. Classic remains the default.
- Added Northern Lights, Thunderstorm, and Rain to the chat list header effects.
- Redesigned the background picker with color previews, additional presets, and a custom color editor.
- Added What's New on first launch after updating and in Plusgram settings.
- Turned the Developers screen into the Developer Council. Tap any of the seven models for a local joke.

## Plusgram 5.0.3

- В настройках Plusgram появился отдельный экран «Разработчики» со списком из семи ИИ-моделей.

## Plusgram 5.0.2

- Restored the upstream TGWS WebSocket routes when the custom data center list is empty.
- Added timeouts for TGWS startup and connection checks.
- Fixed TGWS shutdown with active connections and aligned Python bootstrap assets with the installed Android ABI.

## Plusgram 5.0.1

- Added a Notifications and Connection screen to Plusgram settings and the sign-in menu. It shows Telegram connectivity, push registration, Android notification and battery settings, and proxy and Bypass status.
- Designed the screen around a prominent connection card, rounded sections, and the active Material You palette in light and dark themes.
- Enabled the Rust text filter for AI responses in the signed APK for ARM64 and x86-64.
- Added publication checks for the release certificate, APK privacy, and upgrades from Plusgram 5.0.0 on Android. The Android 15 upgrade test preserved notification permission and opened both the new screen and Material 3 settings.

## Plusgram 5.0.0

База обновлена до [exteraless 13.1.1 beta 15](https://github.com/exteraless/exteraless/releases/tag/v13.1.1-beta15).

- Плеер Material 3 получил редактор стиля, новые настройки мини-плеера и управление очередью.
- Обновлена запись кружков, включая переключение камер на поддерживаемых устройствах.
- Перенесены улучшения уведомлений, режима «Призрак», AyuMoments и совместимости плагинов.
- Сохранены системная палитра Material You, единый фон карточек настроек и отключение Liquid Glass.
- Bypass по-прежнему использует Flowseal TGWS.

APK для Android 7.0 и новее с поддержкой ARM64 и x86‑64.
