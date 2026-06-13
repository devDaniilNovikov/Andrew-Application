#!/usr/bin/env python3
import os
import sys
import time
import json
import asyncio
import subprocess
import logging
import requests

# Bypasses local proxy routing for loopback websocket connections of the SDK
os.environ["no_proxy"] = "localhost,127.0.0.1"

# Enable logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")

try:
    from google.antigravity import Agent, LocalAgentConfig
    from google.antigravity.hooks import policy
except ImportError:
    logging.error("google-antigravity is not installed. Please run: pip install google-antigravity")
    sys.exit(1)

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

SYSTEM_INSTRUCTIONS = """Вы — elite Senior Android Software Engineer & Debugging Expert 'debugger'. Ваша роль — автоматически исправлять баги, найденные ревьюером, на ветке 'fix' для достижения оценки не менее 95/100.

ВАЖНО: Все ваши отчеты, коммиты и комментарии должны быть строго на русском языке.

При исправлении багов:
1. Внимательно проанализируйте отчет ревьюера и файлы проекта.
2. Внесите исправления во все файлы с багами напрямую с помощью ваших инструментов редактирования.
3. Код должен соответствовать современным гайдлайнам Android, Jetpack Compose и Clean Architecture.
4. Обязательно убедитесь в отсутствии мерцания (flicker) UI, SSoT (Single Source of Truth) во ViewModel и правильном асинхронном получении данных.
5. После внесения исправлений выполните коммит изменений через git:
   - git add <измененные_файлы>
   - git commit -m "Фикс: [детальное описание исправлений]"
6. Выполните push в ветку 'fix':
   - git push origin fix
7. Завершите работу и выдайте подробный отчет о проделанной работе."""

def send_telegram_notification(token, chat_id, text):
    url = f"https://api.telegram.org/bot{token}/sendMessage"
    payload = {
        "chat_id": chat_id,
        "text": text,
        "parse_mode": "Markdown"
    }
    try:
        res = requests.post(url, json=payload, timeout=15)
        if res.status_code == 200:
            logging.info("Sent Telegram notification successfully!")
        else:
            logging.error(f"Failed to send Telegram message: {res.text}")
    except Exception as e:
        logging.error(f"Error sending Telegram notification: {e}")

async def run_debugger_agent(workspace_dir, review_data):
    score = review_data.get("score", 0)
    stage = review_data.get("stage_name", "Unknown Stage")
    summary = review_data.get("summary", "")
    issues = review_data.get("bugs_and_issues", [])
    recs = review_data.get("recommendations", [])

    issues_text = ""
    for idx, issue in enumerate(issues, 1):
        issues_text += f"{idx}. [{issue.get('severity')}] `{issue.get('file_path')}` (Строка {issue.get('line_number')}):\n   {issue.get('description')}\n"

    recs_text = "\n".join([f"- {r}" for r in recs])

    prompt = (
        f"Привет, Дебаггер! Ревьюер прислал отчет о пулл-реквесте с оценкой {score}/100 на этапе '{stage}'.\n"
        f"Эта оценка ниже целевой планки 95/100, поэтому мы ОБЯЗАНЫ исправить все найденные баги!\n\n"
        f"### Резюме отчета:\n{summary}\n\n"
        f"### Список обнаруженных багов:\n{issues_text}\n"
        f"### Рекомендации:\n{recs_text}\n\n"
        f"Пожалуйста, проанализируй файлы проекта, исправь все указанные баги, сделай коммит (git commit) на русском языке и запушь изменения в ветку 'fix' (git push origin fix).\n"
        f"После завершения выведи отчет о проделанной работе."
    )

    config = LocalAgentConfig(
        model="gemini-3.1-pro-preview",
        system_instructions=SYSTEM_INSTRUCTIONS,
        workspaces=[workspace_dir],
        policies=[policy.allow_all()] # Позволяет свободно редактировать файлы и вызывать git
    )

    logging.info("Initializing Debugger Agent...")
    async with Agent(config=config) as agent:
        response = await agent.chat(prompt)
        text_output = await response.text()
        logging.info("Debugger Agent successfully finished execution!")
        return text_output

async def main():
    tg_token = os.environ.get("TELEGRAM_BOT_TOKEN")
    tg_chat_id = os.environ.get("TELEGRAM_CHAT_ID")
    workspace_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    review_file = os.path.join(workspace_dir, "scripts", "latest_review.json")

    logging.info(f"Starting Debugger Daemon on workspace: {workspace_dir}")
    logging.info(f"Watching file: {review_file}")

    last_processed_mtime = 0

    # Если файл уже существует на старте, запишем его mtime, чтобы не реагировать на старые ревью при перезапуске
    if os.path.exists(review_file):
        last_processed_mtime = os.path.getmtime(review_file)
        logging.info(f"Review file exists. Starting monitoring from mtime {last_processed_mtime}")

    while True:
        try:
            if os.path.exists(review_file):
                current_mtime = os.path.getmtime(review_file)
                if current_mtime > last_processed_mtime:
                    logging.info("Detected new review event! Reading report...")
                    
                    # Читаем ревью
                    with open(review_file, "r", encoding="utf-8") as f:
                        review_data = json.load(f)
                    
                    score = review_data.get("score", 0)
                    stage = review_data.get("stage_name", "Unknown Stage")
                    
                    logging.info(f"Latest review score: {score}/100 for stage '{stage}'")
                    
                    if score < 95:
                        logging.info(f"Score {score}/100 < 95/100. Activating debugger agent...")
                        
                        if tg_token and tg_chat_id:
                            msg = (
                                f"🛠 *AGY DEBUGGER DAEMON*\n\n"
                                f"Получено новое уведомление от Reviewer'а!\n"
                                f"📅 *Этап*: {stage}\n"
                                f"⭐ *Оценка*: `{score}/100` (Меньше целевого порога 95/100)\n\n"
                                f"🔴 *Решение*: Принято в работу! Запускаю авто-исправление багов для достижения оценки >= 95/100..."
                            )
                            send_telegram_notification(tg_token, tg_chat_id, msg)
                        
                        # Запускаем отладчика
                        report = await run_debugger_agent(workspace_dir, review_data)
                        
                        if tg_token and tg_chat_id:
                            msg = (
                                f"✅ *AGY DEBUGGER DAEMON*\n\n"
                                f"Баги успешно устранены агентом debugger!\n"
                                f"📝 *Отчет отладчика*:\n\n{report[:3000]}"
                            )
                            send_telegram_notification(tg_token, tg_chat_id, msg)
                    else:
                        logging.info(f"Score {score}/100 >= 95/100. No debugging needed!")
                        
                        if tg_token and tg_chat_id:
                            msg = (
                                f"🛠 *AGY DEBUGGER DAEMON*\n\n"
                                f"Получено новое уведомление от Reviewer'а!\n"
                                f"📅 *Этап*: {stage}\n"
                                f"⭐ *Оценка*: `{score}/100` (Удовлетворяет критерию >= 95/100)\n\n"
                                f"🟢 *Решение*: Оценка отличная, баг-фикс не требуется! Проект готов к релизу."
                            )
                            send_telegram_notification(tg_token, tg_chat_id, msg)
                    
                    last_processed_mtime = current_mtime
            
        except Exception as e:
            logging.error(f"Error in debugger daemon: {e}", exc_info=True)
            
        await asyncio.sleep(5)

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logging.info("Debugger Daemon stopped.")
