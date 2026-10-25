# Проверка Plusgram — 25 октября 2026

Изменения выполнены в `D:\Plusgram\plusgram`.

## Успешные проверки

- `NATIVE_TARGET=SKIP ./gradlew help --offline --console=plain`: BUILD SUCCESSFUL, конфигурация Gradle принимается; Firebase отключён без собственных ключей.
- Android Build Tools 36.0.0, `aapt2 compile --dir TMessagesProj/src/main/res -o build/plusgram-resources.zip`: все ресурсы скомпилированы, код возврата 0.
- Исправлены выявленные AAPT2 дефекты исходных строк: несколько непозиционных подстановок, смешанная нумерация аргументов, лишний обратный слеш между XML-элементами и literal `%` в описаниях рейтинга.
- XML-ресурсы разобраны; пути всех перенесённых Java/Kotlin-классов совпадают с package; launcher alias PlusgramIcon связан с новыми иконками.
- 10 PNG лаунчера проверены: 48, 72, 96, 144 и 192 px, обычный и круглый варианты.
- `git diff --check`: без ошибок пробелов.
- `local.properties` и сохранённый исходный Firebase-файл исключены из Git.
- `py -X utf8 -m pytest tests -q --disable-warnings -k "not test_symlink_to_settings_does_not_bypass"`: 155 passed, 35 skipped, 1 deselected.
- Новый `test_material_you.py` компилирует реальный MaterialYouController через javac и проверяет системный/фиксированные режимы, восстановление предыдущей пользовательской темы, возврат автоматического режима и отсутствие Monet на Android ниже 12. Android и Theme в этом JVM-тесте заменены минимальной моделью; это не тест рендеринга на устройстве.

Полный запуск тестов выявил один отказ окружения: `test_symlink_to_settings_does_not_bypass` не может создать symlink в Windows (WinError 1314). Проверка не удалена и не изменена, при повторной проверке исключена явно. Остальные пропуски заданы существующей тестовой системой для недоступных корпусов/референсов.

## Что ещё не подтверждено

Рабочий APK не собран. Полная сборка упёрлась в незавершённую установку NDK 27.2.12479018 (нет `source.properties`). Проверка Gradle-ресурсов с NATIVE_TARGET=SKIP затем остановилась из-за отсутствия Android SDK 35 для Media3. На компьютере также не найден Python 3.11, необходимый текущему Chaquopy runtime. Полный native runtime использует POSIX Bash-скрипт; рекомендуемая среда сборки — Linux/WSL2 или настроенный GitHub Actions.

Экранные состояния на Android, работа JNI, установка APK, подпись и доставка push-уведомлений не проверялись. GitHub Actions не запускался, изменения не отправлялись на GitHub. Поддержка динамических цветов опирается на существующий движок Monet; Material 3 подключён к уже имеющимся компонентам интерфейса, а не представляет перенос всего Telegram UI на Compose.

Следующая проверка на устройстве: Android 11 (формы и прежняя палитра), Android 12+ (смена обоев, day/night, AMOLED, возврат прежней темы), холодный запуск, изменение размера шрифта, landscape/tablet, переключение иконки и системные themed icons на Android 13+.

Технические справочники: [Android dynamic colors](https://developer.android.com/develop/ui/views/theming/dynamic-colors), [Application ID и namespace](https://developer.android.com/build/configure-app-module).
