import asyncio
import json
import logging
import os
from pathlib import Path
from typing import Dict, Any, List

import aiohttp
from aiogram import Bot, Dispatcher, F, types
from aiogram.filters import Command, CommandStart
from aiogram.types import (
    InlineKeyboardButton,
    InlineKeyboardMarkup,
    LabeledPrice,
    PreCheckoutQuery,
    Message,
)

# Load environment variables
try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

BOT_TOKEN = os.getenv("BOT_TOKEN", "YOUR_BOT_TOKEN_HERE")
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN", "")
GITHUB_REPO = os.getenv("GITHUB_REPO", "IEONDDD/PlusGram")
CREATOR_ID = int(os.getenv("CREATOR_ID", "1084312196"))
PRICE_STARS = int(os.getenv("PRICE_STARS", "10"))

# Path to local badges.json
SCRIPT_DIR = Path(__file__).parent.resolve()
LOCAL_BADGES_PATH = SCRIPT_DIR.parent / "badges.json"
if not LOCAL_BADGES_PATH.exists():
    LOCAL_BADGES_PATH = SCRIPT_DIR / "badges.json"

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

bot = Bot(token=BOT_TOKEN)
dp = Dispatcher()


def load_badges() -> Dict[str, Any]:
    if LOCAL_BADGES_PATH.exists():
        try:
            with open(LOCAL_BADGES_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            logger.error(f"Error loading local badges.json: {e}")
    return {"version": 1, "users": {str(CREATOR_ID): {"badges": ["creator", "supporter"], "selected": "creator"}}}


def save_badges_locally(data: Dict[str, Any]):
    try:
        with open(LOCAL_BADGES_PATH, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        logger.info(f"Saved badges locally to {LOCAL_BADGES_PATH}")
    except Exception as e:
        logger.error(f"Failed to save badges locally: {e}")


async def sync_badges_to_github(data: Dict[str, Any]) -> bool:
    # 1. Try local git push first (uses local repository git credentials)
    try:
        repo_dir = str(LOCAL_BADGES_PATH.parent)
        proc_add = await asyncio.create_subprocess_exec(
            "git", "add", "badges.json",
            cwd=repo_dir,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE
        )
        await proc_add.communicate()

        proc_commit = await asyncio.create_subprocess_exec(
            "git", "commit", "-m", "Update badges.json via bot",
            cwd=repo_dir,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE
        )
        await proc_commit.communicate()

        proc_push = await asyncio.create_subprocess_exec(
            "git", "push", "origin", "main",
            cwd=repo_dir,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE
        )
        stdout, stderr = await proc_push.communicate()
        if proc_push.returncode == 0:
            logger.info("Successfully pushed badges.json to GitHub via git CLI!")
            return True
        else:
            logger.warning(f"Git push returned code {proc_push.returncode}: {stderr.decode()}")
    except Exception as e:
        logger.warning(f"Git CLI push failed: {e}")

    # 2. Fallback to GitHub REST API if GITHUB_TOKEN is configured
    if not GITHUB_TOKEN or not GITHUB_REPO:
        logger.warning("GITHUB_TOKEN or GITHUB_REPO not set. Skipping GitHub API commit.")
        return False

    url = f"https://api.github.com/repos/{GITHUB_REPO}/contents/badges.json"
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json",
        "User-Agent": "PlusgramStarsBot",
    }

    try:
        async with aiohttp.ClientSession() as session:
            # 1. Get existing file sha
            sha = None
            async with session.get(url, headers=headers) as resp:
                if resp.status == 200:
                    res_json = await resp.json()
                    sha = res_json.get("sha")

            # 2. Put updated content
            import base64
            content_bytes = json.dumps(data, indent=2, ensure_ascii=False).encode("utf-8")
            payload = {
                "message": "Add supporter badge via Telegram Stars",
                "content": base64.b64encode(content_bytes).decode("utf-8"),
                "branch": "main",
            }
            if sha:
                payload["sha"] = sha

            async with session.put(url, headers=headers, json=payload) as resp:
                if resp.status in (200, 201):
                    logger.info("Successfully synced badges.json to GitHub repository!")
                    return True
                else:
                    text = await resp.text()
                    logger.error(f"Failed to push to GitHub: status={resp.status}, response={text}")
                    return False
    except Exception as e:
        logger.error(f"Error syncing to GitHub: {e}")
        return False


def add_supporter(user_id: int) -> bool:
    data = load_badges()
    users = data.setdefault("users", {})
    uid_str = str(user_id)

    if uid_str not in users:
        users[uid_str] = {
            "badges": ["supporter"],
            "selected": "supporter",
        }
    else:
        badges: List[str] = users[uid_str].get("badges", [])
        if "supporter" not in badges:
            badges.append("supporter")
        users[uid_str]["badges"] = badges
        if not users[uid_str].get("selected") or users[uid_str].get("selected") == "none":
            users[uid_str]["selected"] = "supporter"

    save_badges_locally(data)
    return True


@dp.message(CommandStart())
async def cmd_start(message: Message):
    keyboard = InlineKeyboardMarkup(
        inline_keyboard=[
            [
                InlineKeyboardButton(
                    text=f"⭐️ Поддержать проект ({PRICE_STARS} Stars)",
                    callback_data="donate_stars"
                )
            ],
            [
                InlineKeyboardButton(
                    text="ℹ️ Мой статус и значки",
                    callback_data="check_status"
                )
            ]
        ]
    )

    text = (
        "👋 **Добро пожаловать в официальный бот поддержки Plusgram!**\n\n"
        "Здесь вы можете поддержать развитие клиента **Plusgram** с помощью Telegram Stars "
        "и получить уникальный значок **Спонсора (1+)** в свой профиль!\n\n"
        "✨ **Что даёт значок?**\n"
        "• Отображение значка 1+ рядом с вашим именем во всех профилях и боковом меню\n"
        "• Цвет значка автоматически адаптируется под вашу тему оформления (Material You)\n"
        "• Всплывающее окно с информацией о вашей поддержке проекта\n"
        "• Возможность включать, отключать и выбирать активный значок в настройках Plusgram\n\n"
        f"Стоимость значка спонсора: **{PRICE_STARS} Telegram Stars** ⭐️"
    )

    await message.answer(text, reply_markup=keyboard, parse_mode="Markdown")


@dp.callback_query(F.data == "donate_stars")
async def send_stars_invoice(callback: types.CallbackQuery):
    await callback.answer()

    prices = [LabeledPrice(label="Спонсор Plusgram (1+)", amount=PRICE_STARS)]

    await callback.message.answer_invoice(
        title="Спонсор Plusgram (Значок 1+)",
        description=(
            "Эксклюзивный значок спонсора в профиль Plusgram. "
            "Цвет автоматически адаптируется под тему оформления!"
        ),
        prices=prices,
        currency="XTR",  # Telegram Stars currency code
        provider_token="",  # Must be empty for Telegram Stars
        payload=f"plusgram_supporter_{callback.from_user.id}",
    )


@dp.callback_query(F.data == "check_status")
async def check_user_status(callback: types.CallbackQuery):
    await callback.answer()
    data = load_badges()
    user_info = data.get("users", {}).get(str(callback.from_user.id))

    if not user_info or not user_info.get("badges"):
        await callback.message.answer(
            "У вас пока нет активных значков Plusgram.\n"
            f"Вы можете получить значок спонсора 1+, поддержав проект на {PRICE_STARS} ⭐️!",
            parse_mode="Markdown"
        )
        return

    badges = user_info.get("badges", [])
    selected = user_info.get("selected", "none")
    badge_names = {
        "creator": "🔨 Создатель Plusgram",
        "supporter": "⭐️ Спонсор Plusgram (1+)",
    }
    badges_str = "\n".join([f"• {badge_names.get(b, b)}" for b in badges])

    await callback.message.answer(
        f"🎉 **Ваши значки Plusgram:**\n\n{badges_str}\n\n"
        f"Текущий выбранный значок: **{badge_names.get(selected, selected)}**\n\n"
        "Вы можете управлять отображением значка в настройках Plusgram:\n"
        "_Настройки -> Значки профиля_",
        parse_mode="Markdown"
    )


@dp.pre_checkout_query()
async def process_pre_checkout(pre_checkout_query: PreCheckoutQuery):
    # Stars payments must always be confirmed within 10 seconds
    await bot.answer_pre_checkout_query(pre_checkout_query.id, ok=True)


@dp.message(F.successful_payment)
async def process_successful_payment(message: Message):
    user_id = message.from_user.id
    stars_amount = message.successful_payment.total_amount
    charge_id = message.successful_payment.telegram_payment_charge_id

    logger.info(f"Payment received! User: {user_id}, Amount: {stars_amount} XTR, Charge ID: {charge_id}")

    # Add user to badges.json
    add_supporter(user_id)

    # Sync to GitHub repo if token provided
    data = load_badges()
    synced = await sync_badges_to_github(data)

    sync_note = (
        "✅ Значок синхронизирован с сервером Plusgram!"
        if synced else
        "ℹ️ Значок сохранён локально и будет синхронизирован в ближайшее время."
    )

    await message.answer(
        "🎉 **Огромное спасибо за поддержку Plusgram!** ⭐️\n\n"
        "Вам успешно выдан эксклюзивный значок **Спонсора (1+)**!\n\n"
        f"{sync_note}\n\n"
        "👉 Чтобы значок появился в приложении, перезапустите Plusgram или откройте:\n"
        "_Настройки -> Значки профиля_\n\n"
        "Цвет значка автоматически совпадает с акцентом вашей темы (Material You)!",
        parse_mode="Markdown"
    )


# Admin commands
@dp.message(Command("addbadge"))
async def cmd_add_badge(message: Message):
    if message.from_user.id != CREATOR_ID:
        return

    parts = message.text.split()
    if len(parts) < 3:
        await message.answer("Использование: `/addbadge <user_id> <badge_name>` (creator / supporter)", parse_mode="Markdown")
        return

    try:
        target_id = int(parts[1])
        badge_name = parts[2]
        data = load_badges()
        users = data.setdefault("users", {})
        u = users.setdefault(str(target_id), {"badges": [], "selected": badge_name})
        if badge_name not in u["badges"]:
            u["badges"].append(badge_name)
        u["selected"] = badge_name
        save_badges_locally(data)
        await sync_badges_to_github(data)
        await message.answer(f"✅ Пользователю `{target_id}` выдан значок `{badge_name}`!", parse_mode="Markdown")
    except Exception as e:
        await message.answer(f"Ошибка: {e}")


@dp.message(F.text)
async def fallback_text(message: Message):
    await cmd_start(message)


async def main():
    logger.info("Starting Plusgram Stars Bot...")
    await dp.start_polling(bot)


if __name__ == "__main__":
    asyncio.run(main())
