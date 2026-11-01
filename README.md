<div align="center">
<img src="assets/logo.png" width="128" alt="Plusgram">

# Plusgram

Кастомный Android-клиент Telegram на базе exteraless / NagramX.

[![Telegram Channel](https://img.shields.io/badge/Telegram-Канал%20с%20обновлениями-2CA5E0?style=flat&logo=telegram&logoColor=white)](https://t.me/OPlusGram)
[![GitHub Release](https://img.shields.io/badge/Release-v4.0.0-blue?logo=github)](https://github.com/IEONDDD/PlusGram/releases)
</div>

- **Telegram-канал (новости и обновления)**: https://t.me/OPlusGram
- **Релизы и готовый APK**: https://github.com/IEONDDD/PlusGram/releases
- Application ID: `com.plusgram.app`
- Исходники: https://github.com/IEONDDD/PlusGram
- Ошибки и предложения: https://github.com/IEONDDD/PlusGram/issues
- Собственный код: `TMessagesProj/src/main/java/com/plusgram/app` и `src/main/kotlin/com/plusgram/app`.
- Пакеты ядра Telegram и namespace `org.telegram.messenger` сохранены для JNI и совместимости API.

## Material You

Настройки → Plusgram → Внешний вид → Цвета Material You: как в системе, светлая, тёмная, AMOLED или возврат к прежней теме. Динамическая палитра доступна с Android 12, обновляется при изменении системных цветов существующим движком Monet.

«Применить стиль Material You» включает восемь стилей Material 3, карточки со скруглением 24 dp и скруглённую плавающую кнопку. На Android 12+ также выбирается системная тема Monet. Отдельные стили, радиус карточек и темы можно затем изменить на этом же экране. На Android 7–11 доступны формы Material 3 без динамической палитры. Выключение цветов возвращает прежние темы, но не отменяет отдельно выбранные формы и элементы.

## Plusgram 4.0

- Функция значков создателя/спонсора, список пользователей и бот выдачи значков удалены.
- Полностью перенесён Bypass из SoundGram: Go-движок, JNI, экран настроек, Cloudflare, FakeTLS, DoH, пулы соединений, контроль сети, автоопределение блокировки, автозапуск и восстановление сервиса.
- На первом запуске 4.0 применяется Material You: динамическая палитра Android 12+, карточки 28 dp, переключатели, слайдеры, шапки чатов, навигация и медиаплеер Material 3. Дальнейшие изменения темы сохраняются.
- Добавлены появления экранов настроек и Bypass, обратная связь кнопок меню и дополнительных ячеек, общая настройка скорости нижних панелей. Системное отключение анимаций учитывается.

Источник Bypass: [soundgram-project/SoundGram](https://github.com/soundgram-project/SoundGram/tree/b489ce80fdc5390844902582d50b0476a6278d78). Версия и изменения интеграции описаны в `third_party/soundgram-bypass/README.md`. Работа обхода зависит от доступности внешних маршрутов и ограничений провайдера.

## Проверка приватности релиза

```sh
python Tools/check_release_privacy.py --apk path/to/Plusgram.apk --report privacy-report.json
```

Проверяются текущие публикуемые файлы и распакованное содержимое APK: токены, приватные ключи, файлы конфигурации, сессии и личные пути сборочной машины. Публичные криптографические тестовые векторы зависимостей учитываются отдельно. Приватный ключ подписи, `local.properties` и учётные данные пользователей не должны попадать в APK или исходники релиза. Telegram API ID/hash и публичная конфигурация Firebase — обязательные клиентские параметры приложения; APK не является хранилищем секретов. Проверка не удаляет данные из прошлых коммитов и старых релизов.

## Сборка

```sh
git clone --recursive --shallow-submodules https://github.com/IEONDDD/PlusGram.git plusgram
cd plusgram
```

Скопируйте `local.properties.example` в `local.properties`. Укажите `sdk.dir`, свой `TELEGRAM_APP_ID` (только цифры) и `TELEGRAM_APP_HASH` (32 шестнадцатеричных символа), **без `<` и `>`**. Данные также читаются из переменных окружения. Публичные ключи Telegram больше не подставляются автоматически.

Параметры текущего форка: JDK 21, Gradle Wrapper 9.7.1, Android SDK 37 и SDK 35 (для Media3), Build Tools 36.0.0, NDK 27.2.12479018, CMake 3.31.6+, Python 3.11 и Go 1.27.3. При необходимости задайте путь к Python в `CHAQUOPY_BUILD_PYTHON`.

Сборка Chaquopy использует Bash-скрипт собственного runtime. Поддерживаются Linux / WSL2 и Windows с Git Bash. На Windows установите Go в PATH и задайте путь к Python 3.11 в `CHAQUOPY_BUILD_PYTHON`. Bypass собирается из Go-исходников автоматически для выбранных ABI, а не загружается в виде готовой библиотеки.

```sh
export NATIVE_TARGET=arm64-v8a
./gradlew :TMessagesProj:assembleDebug
# Оба ABI: NATIVE_TARGET=universal
# С подписанным release.keystore и параметрами подписи:
./gradlew :TMessagesProj:assembleRelease
```

APK: `TMessagesProj/build/outputs/apk/`. Имена начинаются с `Plusgram`. `NATIVE_TARGET=SKIP` предназначен только для проверки Java/ресурсов: APK без нативных библиотек не является рабочей сборкой приложения.

### Firebase, карты и подпись

Firebase включается только при наличии `TMessagesProj/google-services.json` с клиентом `com.plusgram.app`. Файл исходного форка не подходит для нового приложения; локально он сохранён как игнорируемый `google-services.upstream.json`. Без собственной конфигурации FCM и Crashlytics выключены; фоновые уведомления зависят от выбранного встроенного push-сервиса. Для Google Maps нужен собственный `MAPS_API_KEY`, ограниченный новым package name и сертификатом подписи.

Ключ подписи: `TMessagesProj/release.keystore`; параметры `KEYSTORE_PASS`, `ALIAS_NAME`, `ALIAS_PASS` — в `local.properties` или окружении. Без ключа исходная конфигурация подписывает APK отладочным ключом. Не публикуйте такую сборку как production-релиз. `local.properties` и ключи исключены из Git.

### GitHub Actions

Workflow `.github/workflows/release.yml` собирает APK на Ubuntu. Добавьте GitHub secrets `TELEGRAM_APP_ID` и `TELEGRAM_APP_HASH`, либо `LOCAL_PROPERTIES` (base64 содержимого properties). Для подписи — `RELEASE_KEYSTORE` (base64 keystore) и параметры подписи внутри `LOCAL_PROPERTIES`. Для Firebase можно добавить шаг восстановления собственной конфигурации до сборки.

Сборка запускается вручную через Actions (`workflow_dispatch`). При выборе `publish=true` релиз также отправляется в Telegram (если заданы секреты `TG_BOT_TOKEN`, `TG_API_ID`, `TG_API_HASH`, `TG_CHAT_ID`).

## Иконка

Исходник: `assets/plusgram-icon-source.png`. Готовые размеры: `mipmap-mdpi` (48 px), `hdpi` (72), `xhdpi` (96), `xxhdpi` (144), `xxxhdpi` (192), файлы `ic_launcher.png` и `ic_launcher_round.png`. Android 8+ использует адаптивные слои, Android 13+ также поддерживает монохромную иконку. Логотип внутри приложения и в README использует ту же иллюстрацию.

```sh
python -m pip install Pillow
python Tools/prepare_plusgram_icons.py assets/plusgram-icon-source.png
```

Монохромный знак в этом инструменте задан вектором отдельно от исходного PNG; при замене рисунка на совершенно другой логотип обновите также его пути.

## Совместимость и авторство

Plusgram основан на [exteraless](https://github.com/exteraless/exteraless), [NagramX](https://github.com/risin42/NagramX) и Telegram Android. Лицензия GPL-2.0, исходные уведомления об авторстве и лицензии зависимостей сохранены. Репозитории submodules не переименованы: это зависимости, а не репозиторий Plusgram.

Имена форматов резервных копий, ключи настроек и старые маршруты ссылок сохранены для совместимости. Плагины, использующие внутренний пакет `app.exteraless` напрямую, потребуют обновления на `com.plusgram.app`. Облачный помощник `CloudStorageHelper` исходного форка всё ещё ссылается на его Telegram-бота; собственный бот не задан.
