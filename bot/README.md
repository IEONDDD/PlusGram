# ⭐️ Plusgram Stars Bot — Выдача значков спонсора за Telegram Stars

Бот для автоматической продажи значков спонсора (1+) в клиенте **Plusgram** за **Telegram Stars (⭐️)**.

---

## 🚀 Возможности
- Приём оплаты в **Telegram Stars** (валюта `XTR`) напрямую в Telegram без посредников.
- Автоматическое добавление пользователя в `badges.json` (локально и с автоматическим коммитом через GitHub API в репозиторий `IEONDDD/PlusGram`).
- Мгновенная активация значка у пользователя в клиенте Plusgram.
- Команды администратора для создателя (`/addbadge <user_id> <badge>`).

---

## 🛠 Настройка бота

### 1. Создание бота в @BotFather
1. Откройте [@BotFather](https://t.me/BotFather) в Telegram.
2. Отправьте команду `/newbot`.
3. Укажите имя (например, `Plusgram Stars`) и юзернейм (например, `PlusgramStarsBot` или любой свободный).
4. Скопируйте полученный **HTTP API Token**.
5. *(Для Telegram Stars не требуется подключать платёжных провайдеров Stripe/ЮKassa — Stars работают нативно из коробки!)*

### 2. Получение GitHub Token (для автоматического обновления badges.json)
1. Перейдите в [GitHub -> Settings -> Developer Settings -> Personal access tokens (classic)](https://github.com/settings/tokens).
2. Нажмите **Generate new token (classic)**.
3. Выберите область прав: `repo` (полный доступ к репозиториям для обновления файлов).
4. Скопируйте сгенерированный токен `ghp_...`.

### 3. Установка и запуск

1. Перейдите в папку `bot`:
   ```bash
   cd bot
   ```

2. Установите зависимости:
   ```bash
   pip install -r requirements.txt
   ```

3. Создайте файл `.env` на основе `.env.example`:
   ```bash
   cp .env.example .env
   ```
   и заполните:
   ```env
   BOT_TOKEN=ваш_токен_бота
   GITHUB_TOKEN=ghp_токен_гитхаба
   GITHUB_REPO=IEONDDD/PlusGram
   CREATOR_ID=1084312196
   PRICE_STARS=10
   ```

4. Запустите бота:
   ```bash
   python bot.py
   ```

---

## 📱 Как работает выдача значков
1. Пользователь заходит в бота и нажимает **"⭐️ Поддержать проект (10 Stars)"**.
2. Telegram открывает нативное окно подтверждения оплаты звёздами.
3. После списания звёзд Telegram отправляет `SuccessfulPayment`.
4. Бот мгновенно записывает Telegram ID пользователя в `badges.json` с бейджем `supporter` и отправляет коммит в репозиторий.
5. При следующем запуске Plusgram подтягивает `badges.json` с GitHub, и у пользователя появляется значок!
