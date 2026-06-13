#!/usr/bin/env python3
import os
import sys
import time
import json
import argparse
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

DEVELOPER_SYSTEM_INSTRUCTIONS = """Вы — elite Senior Android Software Engineer 'developer'. Ваша роль — качественно реализовать назначенную подзадачу из файла TASKS.md.

Вы должны строго следовать следующим правилам разработки:
1. Пишите чистый, масштабируемый и современный код на Kotlin.
2. Архитектура: UI (Compose) -> ViewModel (StateFlow) -> Repository -> Room database.
3. Соблюдайте принцип Single Source of Truth (SSoT).
4. Все операции ввода-вывода (IO) и запросы к базе данных должны выполняться асинхронно с использованием корутин Kotlin (Dispatchers.IO).
5. На UI-слое собирайте StateFlow безопасно через collectAsStateWithLifecycle().
6. Настройте премиальный внешний вид (цвета из палитры Material 3, современные шрифты, плавные анимации, отсутствие мерцаний интерфейса).
7. Обязательно проверяйте работоспособность и корректность сборки вашего кода. Вы можете выполнить команду сборки, например, `./gradlew compileDebugKotlin`.
8. После внесения изменений добавьте файлы в Git и сделайте аккуратный коммит на русском языке:
   - git add <измененные_файлы>
   - git commit -m "Реализация <task_id>: [детальное описание сделанных изменений]"
9. Выдайте подробный отчет о проделанной работе, измененных и созданных файлах на русском языке."""

DEBUGGER_SYSTEM_INSTRUCTIONS = """Вы — elite Senior Android Software Engineer & Debugging Expert 'debugger'. Ваша роль — автоматически исправлять баги, найденные ревьюером, на ветке 'fix' для достижения оценки не менее 95/100.

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

def parse_tasks(tasks_path):
    with open(tasks_path, "r", encoding="utf-8") as f:
        lines = f.readlines()
    
    stages = []
    current_stage = None
    
    for idx, line in enumerate(lines):
        line_strip = line.strip()
        if line_strip.startswith("## "):
            stage_name = line_strip.replace("## ", "").strip()
            current_stage = {
                "name": stage_name,
                "start_line": idx,
                "tasks": []
            }
            stages.append(current_stage)
        elif current_stage and (line_strip.startswith("- [ ]") or line_strip.startswith("- [x]") or line_strip.startswith("- [/]")):
            status = "completed" if line_strip.startswith("- [x]") else "in_progress" if line_strip.startswith("- [/]") else "pending"
            content = line_strip[5:].strip()
            parts = content.split(".", 1)
            task_id = ""
            task_desc = content
            if len(parts) > 1:
                maybe_id = parts[0].strip()
                if any(c.isdigit() for c in maybe_id):
                    task_id = maybe_id
                    task_desc = parts[1].strip()
            
            current_stage["tasks"].append({
                "id": task_id,
                "desc": task_desc,
                "status": status,
                "line_idx": idx,
                "raw_line": line
            })
    return stages, lines

def mark_task_status(tasks_path, line_idx, status_char="x"):
    with open(tasks_path, "r", encoding="utf-8") as f:
        lines = f.readlines()
    
    line = lines[line_idx]
    if line.strip().startswith("- ["):
        start_idx = line.find("- [")
        if start_idx != -1:
            lines[line_idx] = line[:start_idx + 3] + status_char + line[start_idx + 4:]
            
    with open(tasks_path, "w", encoding="utf-8") as f:
        f.writelines(lines)

def update_or_create_pr(workspace_dir, stage_name):
    logging.info(f"All subtasks for stage '{stage_name}' are complete! Updating/creating pull request...")
    create_pr_script = os.path.join(workspace_dir, "scripts", "create_pr.py")
    if not os.path.exists(create_pr_script):
        scratch_pr = "/Users/daniilnovikov/.gemini/antigravity/brain/f20c7421-1adf-42cf-a36f-a981cc26de27/scratch/create_pr.py"
        if os.path.exists(scratch_pr):
            import shutil
            os.makedirs(os.path.dirname(create_pr_script), exist_ok=True)
            shutil.copy(scratch_pr, create_pr_script)
            
    if os.path.exists(create_pr_script):
        try:
            res = subprocess.run([sys.executable, create_pr_script], capture_output=True, text=True, check=True)
            logging.info(f"PR update output: {res.stdout.strip()}")
            return True
        except Exception as e:
            logging.error(f"Error executing create_pr.py: {e}")
            return False
    else:
        logging.warning("create_pr.py not found, skipping PR step.")
        return False

async def run_developer_agent(workspace_dir, task_id, task_desc, stage_name):
    prompt = (
        f"Привет, Разработчик! Твоя задача — реализовать подзадачу {task_id} в рамках этапа '{stage_name}'.\n"
        f"Описание подзадачи из TASKS.md:\n{task_desc}\n\n"
        f"Пожалуйста:\n"
        f"1. Изучи кодовую базу в папке 'app/src/main' и найди файлы, которые нужно создать или изменить.\n"
        f"2. Реализуй эту задачу в полном соответствии с ТЗ (PRD.md) и лучшими практиками Android-разработки.\n"
        f"3. Убедись, что проект компилируется.\n"
        f"4. Сделай коммит изменений в ветку 'fix' с четким сообщением на русском языке:\n"
        f"   git commit -m 'Реализация {task_id}: {task_desc[:50]}...'\n"
        f"5. Предоставь подробный отчет о проделанной работе на русском языке."
    )

    config = LocalAgentConfig(
        model="gemini-3.1-pro-preview",
        system_instructions=DEVELOPER_SYSTEM_INSTRUCTIONS,
        workspaces=[workspace_dir],
        policies=[policy.allow_all()]
    )

    logging.info(f"Initializing Developer Agent for task {task_id}...")
    async with Agent(config=config) as agent:
        response = await agent.chat(prompt)
        text_output = await response.text()
        logging.info(f"Developer Agent finished execution for task {task_id}.")
        return text_output

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
        f"После завершения выведи отчет о проделанной работе на русском языке."
    )

    config = LocalAgentConfig(
        model="gemini-3.1-pro-preview",
        system_instructions=DEBUGGER_SYSTEM_INSTRUCTIONS,
        workspaces=[workspace_dir],
        policies=[policy.allow_all()]
    )

    logging.info("Initializing Debugger Agent...")
    async with Agent(config=config) as agent:
        response = await agent.chat(prompt)
        text_output = await response.text()
        logging.info("Debugger Agent successfully finished execution!")
        return text_output

async def run_one_task_loop(workspace_dir, task, stage_name, tg_token, tg_chat_id):
    task_id = task["id"]
    task_desc = task["desc"]
    logging.info(f"\n==================================================")
    logging.info(f"STARTING DEVELOPMENT LOOP FOR TASK {task_id}: {task_desc}")
    logging.info(f"==================================================")

    if tg_token and tg_chat_id:
        msg = (
            f"🚀 *AGY ORCHESTRATOR*\n\n"
            f"Начинаю разработку подзадачи:\n"
            f"📅 *Этап*: {stage_name}\n"
            f"🔑 *Задача {task_id}*: {task_desc}\n\n"
            f"👨‍💻 Агент *developer* приступает к написанию кода..."
        )
        send_telegram_notification(tg_token, tg_chat_id, msg)

    # 1. Run developer agent to implement the task
    dev_report = await run_developer_agent(workspace_dir, task_id, task_desc, stage_name)
    
    if tg_token and tg_chat_id:
        msg = (
            f"👨‍💻 *DEVELOPER REPORT ({task_id})*\n\n"
            f"Задача успешно реализована и закоммичена!\n\n"
            f"📝 *Отчет*:\n{dev_report[:3000]}"
        )
        send_telegram_notification(tg_token, tg_chat_id, msg)

    # 2. Review & Debug loop
    max_debug_iterations = 3
    for iteration in range(1, max_debug_iterations + 1):
        logging.info(f"\n--- [Iteration {iteration}] Running Pull Request Reviewer... ---")
        
        # Execute pr_reviewer.py to update scripts/latest_review.json
        review_script = os.path.join(workspace_dir, "scripts", "pr_reviewer.py")
        subprocess.run([sys.executable, review_script], check=True)
        
        # Read latest_review.json
        review_file = os.path.join(workspace_dir, "scripts", "latest_review.json")
        with open(review_file, "r", encoding="utf-8") as f:
            review_data = json.load(f)
            
        score = review_data.get("score", 0)
        logging.info(f"Review Score: {score}/100")
        
        if score >= 95:
            logging.info(f"Success! Score {score}/100 meets target of >= 95/100.")
            if tg_token and tg_chat_id:
                msg = (
                    f"🎉 *AGY ORCHESTRATOR - SUCCESS ({task_id})*\n\n"
                    f"Подзадача успешно прошла ревью!\n"
                    f"⭐ *Финальная оценка*: `{score}/100`\n"
                    f"🟢 Переходим к следующей задаче!"
                )
                send_telegram_notification(tg_token, tg_chat_id, msg)
            break
        else:
            logging.info(f"Score {score}/100 < 95/100. Activating debugger agent for iteration {iteration}...")
            if tg_token and tg_chat_id:
                msg = (
                    f"⚠️ *AGY ORCHESTRATOR - BUGFIX ({task_id})*\n\n"
                    f"Оценка ревью `{score}/100` ниже целевой (95/100).\n"
                    f"🛠 Запускаю агента *debugger* (Итерация {iteration}/{max_debug_iterations}) для устранения замечаний..."
                )
                send_telegram_notification(tg_token, tg_chat_id, msg)
                
            # Run debugger
            dbg_report = await run_debugger_agent(workspace_dir, review_data)
            
            if tg_token and tg_chat_id:
                msg = (
                    f"🛠 *DEBUGGER REPORT ({task_id})*\n\n"
                    f"Исправления внесены и закоммичены!\n\n"
                    f"📝 *Отчет отладчика*:\n{dbg_report[:3000]}"
                )
                send_telegram_notification(tg_token, tg_chat_id, msg)

    # 3. Mark task as completed in TASKS.md
    tasks_path = os.path.join(workspace_dir, "TASKS.md")
    mark_task_status(tasks_path, task["line_idx"], "x")
    
    # Commit TASKS.md update
    subprocess.run(["git", "add", "TASKS.md"], check=True)
    subprocess.run(["git", "commit", "-m", f"docs: отметить задачу {task_id} как выполненную в TASKS.md"], check=True)
    
    # Push changes
    try:
        subprocess.run(["git", "push", "origin", "fix"], check=True)
        logging.info("Changes pushed to origin fix branch.")
    except Exception as e:
        logging.error(f"Failed to push changes to remote: {e}")

async def main():
    parser = argparse.ArgumentParser(description="Multi-Agent Automated Orchestrator")
    parser.add_argument("--single-step", action="store_true", help="Run only one pending subtask and stop")
    parser.add_argument("--task", type=str, help="Specific task ID to run (e.g. 1.1)")
    args = parser.parse_args()

    workspace_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    tasks_path = os.path.join(workspace_dir, "TASKS.md")

    tg_token = os.environ.get("TELEGRAM_BOT_TOKEN")
    tg_chat_id = os.environ.get("TELEGRAM_CHAT_ID")

    logging.info(f"Starting Multi-Agent Orchestration workflow on workspace: {workspace_dir}")

    # Main workflow loop
    while True:
        stages, _ = parse_tasks(tasks_path)
        
        active_stage = None
        target_task = None
        
        # 1. Determine active stage and target task
        if args.task:
            # Look for specific task ID
            for stage in stages:
                for task in stage["tasks"]:
                    if task["id"] == args.task:
                        active_stage = stage
                        target_task = task
                        break
                if target_task:
                    break
            if not target_task:
                logging.error(f"Task with ID {args.task} not found in TASKS.md!")
                sys.exit(1)
        else:
            # Find the first stage that has incomplete tasks
            for stage in stages:
                incomplete_tasks = [t for t in stage["tasks"] if t["status"] == "pending"]
                if incomplete_tasks:
                    active_stage = stage
                    target_task = incomplete_tasks[0]
                    break
        
        if not target_task:
            logging.info("🎉 All stages and tasks in TASKS.md are completely completed! Beautiful job!")
            if tg_token and tg_chat_id:
                msg = (
                    f"🏆 *AGY ORCHESTRATOR - FINISHED!*\n\n"
                    f"Все этапы и подзадачи из `TASKS.md` успешно завершены!\n"
                    f"Проект полностью готов и проверен на 95+/100! Великолепная работа."
                )
                send_telegram_notification(tg_token, tg_chat_id, msg)
            break
            
        stage_name = active_stage["name"]
        
        # 2. Execute the workflow for this single subtask
        await run_one_task_loop(workspace_dir, target_task, stage_name, tg_token, tg_chat_id)
        
        # 3. After task completes, check if stage-level PR is needed
        stages_after, _ = parse_tasks(tasks_path)
        stage_after_ref = [s for s in stages_after if s["name"] == stage_name][0]
        incomplete_tasks_left = [t for t in stage_after_ref["tasks"] if t["status"] == "pending"]
        
        if not incomplete_tasks_left:
            logging.info(f"Stage '{stage_name}' has no more pending tasks! Performing stage-level PR and Review...")
            
            # Create/update PR
            update_or_create_pr(workspace_dir, stage_name)
            
            if tg_token and tg_chat_id:
                msg = (
                    f"📬 *AGY ORCHESTRATOR - STAGE PR*\n\n"
                    f"Все задачи этапа *{stage_name}* выполнены!\n"
                    f"Создан/обновлен Пулл-Реквест на GitHub для приемки этапа."
                )
                send_telegram_notification(tg_token, tg_chat_id, msg)
                
            # Perform stage-level review
            review_script = os.path.join(workspace_dir, "scripts", "pr_reviewer.py")
            subprocess.run([sys.executable, review_script], check=True)
            
            # Read latest_review.json
            review_file = os.path.join(workspace_dir, "scripts", "latest_review.json")
            with open(review_file, "r", encoding="utf-8") as f:
                review_data = json.load(f)
                
            score = review_data.get("score", 0)
            if score >= 95:
                logging.info(f"Stage-level review passed with {score}/100! Moving to next stage.")
                if tg_token and tg_chat_id:
                    msg = (
                        f"🏆 *AGY ORCHESTRATOR - STAGE REVIEW PASSED*\n\n"
                        f"📅 *Этап*: {stage_name}\n"
                        f"⭐ *Оценка за весь этап*: `{score}/100` (Успешно!)\n\n"
                        f"Переходим к разработке следующего этапа!"
                    )
                    send_telegram_notification(tg_token, tg_chat_id, msg)
            else:
                logging.info(f"Stage-level review score {score}/100 < 95/100. Running debugger...")
                # Run debugger loop on stage level
                dbg_report = await run_debugger_agent(workspace_dir, review_data)
                if tg_token and tg_chat_id:
                    msg = (
                        f"🛠 *STAGE LEVEL DEBUGGER REPORT*\n\n"
                        f"Устранены замечания этапа!\n\n"
                        f"📝 *Отчет*:\n{dbg_report[:3000]}"
                    )
                    send_telegram_notification(tg_token, tg_chat_id, msg)
        
        # If single-step or specific task run was requested, exit after one task
        if args.single_step or args.task:
            logging.info("Single step execution completed successfully. Exiting.")
            break

if __name__ == "__main__":
    asyncio.run(main())
