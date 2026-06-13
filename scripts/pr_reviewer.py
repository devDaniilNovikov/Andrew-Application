#!/usr/bin/env python3
import os
# Bypasses local proxy routing for loopback websocket connections of the SDK
# Triggering automated workflow runs on GitHub Actions
os.environ["no_proxy"] = "localhost,127.0.0.1"

import asyncio
import sys
import subprocess
import json
import pydantic
import requests

# Enable logging to show SDK activity
import logging
logging.basicConfig(level=logging.WARNING)


try:
    from google.antigravity import Agent, LocalAgentConfig
except ImportError:
    print("Error: google-antigravity package is not installed. Please install it using pip.", file=sys.stderr)
    sys.exit(1)

# Load .env file automatically if present
try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

# Define the target schema using Pydantic
class CodeIssue(pydantic.BaseModel):
    file_path: str
    line_number: int
    severity: str  # "HIGH", "MEDIUM", "LOW"
    description: str

class PullRequestReview(pydantic.BaseModel):
    stage_name: str  # e.g., "Этап 1. Слой данных"
    score: int       # Score from 0 to 100
    summary: str
    bugs_and_issues: list[CodeIssue]
    recommendations: list[str]

SYSTEM_INSTRUCTIONS = """You are a meticulous Senior Android QA & QA Architect. Your job is to conduct comprehensive Pull Request (PR) code-reviews, identify bugs, security concerns, memory leaks, and Room database issues, and then score the implementation.

When reviewing the code changes (git diff):
1. Carefully analyze each modified line in the diff.
2. Identify the active development stage name based on the modified files, contents, and reference tasks (e.g. "Этап 0. Каркас проекта", "Этап 1. Слой данных", "Этап 2. Навигация").
3. Rate the quality, correctness, and PRD compliance of the code changes on a scale of 0 to 100.
4. Output your full review conforming precisely to the requested response schema, including:
   - `stage_name`: The identified development stage.
   - `score`: The assigned score out of 100 (integer).
   - `summary`: A high-level overview of the pull request changes.
   - `bugs_and_issues`: A list of found issues (file path, line number, severity, description).
   - `recommendations`: A list of suggestions for improvement."""

def get_git_diff():
    # Exclude non-source, binary, or unrelated files to reduce prompt size and avoid model formatting errors
    exclusions = [
        "--", ".",
        ":(exclude)*.jar",
        ":(exclude)gradlew*",
        ":(exclude)dashboard.html",
        ":(exclude)scripts/*",
        ":(exclude).github/*",
        ":(exclude).gitignore",
        ":(exclude)evaluations.json",
    ]
    
    base_ref = os.environ.get("GITHUB_BASE_REF")
    if base_ref:
        print(f"Detected GitHub Actions pull request. Base branch: {base_ref}")
        for base in [f"origin/{base_ref}", base_ref]:
            try:
                cmd = ["git", "diff", f"{base}...HEAD"] + exclusions
                print(f"Executing: {' '.join(cmd)}")
                result = subprocess.run(cmd, capture_output=True, text=True, check=True)
                if result.stdout.strip():
                    return result.stdout
            except Exception as e:
                print(f"Warning: git diff with base '{base}' failed: {e}", file=sys.stderr)

    # Standard fallbacks with exclusions
    targets = ["origin/dev", "dev", "origin/main", "main"]
    for target in targets:
        try:
            cmd = ["git", "diff", f"{target}...HEAD"] + exclusions
            result = subprocess.run(cmd, capture_output=True, text=True, check=True)
            if result.stdout.strip():
                return result.stdout
        except Exception:
            pass

    try:
        cmd = ["git", "diff", "HEAD~1"] + exclusions
        result = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return result.stdout
    except Exception:
        return ""

def resolve_telegram_chat_id(token):
    """Automatically queries Telegram getUpdates to find the user's Chat ID."""
    print("Attempting to automatically resolve Telegram Chat ID...")
    url = f"https://api.telegram.org/bot{token}/getUpdates"
    try:
        response = requests.get(url, timeout=10)
        data = response.json()
        if data.get("ok") and data.get("result"):
            # Fetch chat ID from the latest message
            for update in reversed(data["result"]):
                if "message" in update:
                    chat_id = update["message"]["chat"]["id"]
                    first_name = update["message"]["chat"].get("first_name", "User")
                    print(f" Found active Telegram Chat ID: {chat_id} ({first_name})")
                    return chat_id
    except Exception as e:
        print(f"Error checking Telegram updates: {e}", file=sys.stderr)
    return None

def save_chat_id_to_env(chat_id):
    """Appends the resolved Chat ID to the .env file."""
    env_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".env")
    try:
        with open(env_path, "a", encoding="utf-8") as f:
            f.write(f"\nTELEGRAM_CHAT_ID={chat_id}\n")
        print(" Saved TELEGRAM_CHAT_ID to .env file!")
    except Exception as e:
        print(f"Error saving Chat ID to .env: {e}", file=sys.stderr)

def send_telegram_message(token, chat_id, review):
    """Sends a formatted Markdown review report to the specified Telegram Chat ID."""
    stage = review.get("stage_name", "Unknown Stage")
    score = review.get("score", 0)
    summary = review.get("summary", "")
    issues = review.get("bugs_and_issues", [])
    recs = review.get("recommendations", [])

    # Format issues
    issues_text = ""
    if not issues:
        issues_text = "🟢 No issues found! Excellent work."
    else:
        for idx, issue in enumerate(issues[:10], 1):
            emoji = "🔴" if issue.get("severity") == "HIGH" else "🟡" if issue.get("severity") == "MEDIUM" else "🔵"
            issues_text += f"{idx}. {emoji} *[{issue.get('severity')}]* `{issue.get('file_path')}`:L{issue.get('line_number')}\n   _{issue.get('description')}_\n"
        if len(issues) > 10:
            issues_text += f"\n...and {len(issues) - 10} more issues."

    # Format recommendations
    recs_text = ""
    for idx, rec in enumerate(recs[:5], 1):
        recs_text += f"• {rec}\n"

    telegram_text = (
        f"🔍 *PULL REQUEST CODE REVIEW*\n\n"
        f"📅 *Stage*: {stage}\n"
        f"⭐ *Score*: `{score}/100`\n\n"
        f"📝 *Summary*:\n{summary}\n\n"
        f"⚠️ *Identified Bugs & Issues*:\n{issues_text}\n\n"
        f"💡 *Recommendations*:\n{recs_text}"
    )

    url = f"https://api.telegram.org/bot{token}/sendMessage"
    payload = {
        "chat_id": chat_id,
        "text": telegram_text,
        "parse_mode": "Markdown"
    }

    try:
        response = requests.post(url, json=payload, timeout=15)
        if response.status_code == 200:
            print(" Sent Code Review report to Telegram!")
        else:
            print(f"Failed to send Telegram message: {response.text}", file=sys.stderr)
    except Exception as e:
        print(f"Error sending Telegram notification: {e}", file=sys.stderr)

async def main():
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("Error: GEMINI_API_KEY environment variable is not set.", file=sys.stderr)
        print("Please export your API key: export GEMINI_API_KEY='your-key-here'", file=sys.stderr)
        sys.exit(1)

    workspace_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    
    # 1. Gather git diff
    print("Extracting git diff...")
    diff_content = get_git_diff()
    
    if not diff_content.strip():
        error_msg = (
            "⚠️ *АНАЛИЗ ПРИОСТАНОВЛЕН: АКТИВНЫХ ИЗМЕНЕНИЙ НЕ НАЙДЕНО*\n\n"
            "В репозитории отсутствуют измененные файлы относительно ветки `dev`.\n\n"
            "💡 *Рекомендация*: Внесите изменения в код Android-приложения, закоммитьте их в свою фича-ветку, и запустите анализ повторно!"
        )
        print("\n" + "="*50)
        print("Ошибка: Активных изменений (pull request / git diff) относительно ветки dev не найдено.")
        print("="*50)
        
        # Resolve Telegram and send error message
        tg_token = os.environ.get("TELEGRAM_BOT_TOKEN")
        tg_chat_id = os.environ.get("TELEGRAM_CHAT_ID")
        
        if tg_token:
            if not tg_chat_id:
                tg_chat_id = resolve_telegram_chat_id(tg_token)
                if tg_chat_id:
                    save_chat_id_to_env(tg_chat_id)
                    os.environ["TELEGRAM_CHAT_ID"] = str(tg_chat_id)
            
            if tg_chat_id:
                url = f"https://api.telegram.org/bot{tg_token}/sendMessage"
                payload = {
                    "chat_id": tg_chat_id,
                    "text": error_msg,
                    "parse_mode": "Markdown"
                }
                try:
                    requests.post(url, json=payload, timeout=15)
                    print(" Sent 'No active PR' notification to Telegram!")
                except Exception as e:
                    print(f"Error sending Telegram notification: {e}", file=sys.stderr)
            else:
                print("\n[TELEGRAM INFO] To receive notifications in Telegram:")
                print("1. Search for your bot in Telegram and click 'Start' (send /start message).")
                print("2. Re-run this script to automatically resolve and save your Chat ID!")
                
        sys.exit(0)

    # 2. Configure the agent with structured response schema
    config = LocalAgentConfig(
        model="gemini-2.5-flash",
        system_instructions=SYSTEM_INSTRUCTIONS,
        workspaces=[workspace_dir],
        response_schema=PullRequestReview,
    )

    print("Initializing Pull Request Reviewer Agent...")
    async with Agent(config=config) as agent:
        prompt = (
            f"Please conduct a comprehensive code review of the following pull request git diff:\n\n"
            f"```diff\n{diff_content}\n```\n\n"
            "Analyze the changes, identify potential bugs or issues, determine the stage name, "
            "and assign an evaluation score out of 100."
        )
        
        response = await agent.chat(prompt)
        
        # Drain the stream to ensure it is completed and fetch raw text
        raw_text = ""
        try:
            raw_text = await response.text()
            print("\n" + "="*50)
            print("--- RAW MODEL RESPONSE TEXT ---")
            print(raw_text)
            print("="*50 + "\n")
        except Exception as e:
            print(f"Warning: Failed to fetch response text: {e}", file=sys.stderr)

        review_data = await response.structured_output()
        
        # Fallback manual parser if SDK structured_output failed but we have raw JSON text
        if not review_data and raw_text:
            print("Warning: structured_output() returned None. Attempting manual JSON extraction from raw text...", file=sys.stderr)
            import re
            # Find JSON block (from first { to last })
            match = re.search(r"(\{.*\})", raw_text, re.DOTALL)
            if match:
                try:
                    review_data = json.loads(match.group(1))
                    print(" Successfully extracted and parsed JSON from raw response text manually!", file=sys.stderr)
                except Exception as je:
                    print(f"Failed to parse extracted JSON manually: {je}", file=sys.stderr)
            else:
                # Let's search for json block markdown
                match_md = re.search(r"```json\s*(.*?)\s*```", raw_text, re.DOTALL)
                if match_md:
                    try:
                        review_data = json.loads(match_md.group(1))
                        print(" Successfully extracted and parsed Markdown JSON block manually!", file=sys.stderr)
                    except Exception as je:
                        print(f"Failed to parse Markdown JSON block manually: {je}", file=sys.stderr)

        # Print conversation steps diagnostics if it still fails
        if not review_data:
            print("\nConversation Steps Diagnostic Details:", file=sys.stderr)
            for i, step in enumerate(agent.conversation._steps):
                print(f"Step {i}: type={step.type}, status={step.status}, error='{step.error}'", file=sys.stderr)
                if step.content:
                    print(f"  Step content (first 200 chars): {step.content[:200]}...", file=sys.stderr)
                if step.structured_output:
                    print(f"  Step structured_output found! Using it as fallback.", file=sys.stderr)
                    review_data = step.structured_output
                    break

    if not review_data:
        print("Error: Failed to obtain structured output from the reviewer agent.", file=sys.stderr)
        sys.exit(1)

    # 3. Print beautiful markdown report
    stage = review_data.get("stage_name", "Unknown Stage")
    score = review_data.get("score", 0)
    summary = review_data.get("summary", "")
    issues = review_data.get("bugs_and_issues", [])
    recs = review_data.get("recommendations", [])

    print("\n" + "="*50)
    print(f" Pull Request Code Review Report")
    print("="*50)
    print(f"\n* **Stage**: {stage}")
    print(f"* **Score**: {score}/100")
    print(f"\n### Summary:\n{summary}")

    print("\n### Identified Bugs & Code Issues:")
    if not issues:
        print(" No issues found. Excellent work!")
    else:
        for idx, issue in enumerate(issues, 1):
            severity_emoji = "🔴" if issue.get("severity") == "HIGH" else "🟡" if issue.get("severity") == "MEDIUM" else "🔵"
            print(f"{idx}. {severity_emoji} **[{issue.get('severity')}]** in `{issue.get('file_path')}` at line {issue.get('line_number')}:")
            print(f"   {issue.get('description')}")

    print("\n### Recommendations:")
    for idx, rec in enumerate(recs, 1):
        print(f"{idx}. {rec}")

    print("\n" + "="*50)

    # 4. Save to evaluations.json as key-value pairs
    evals_path = os.path.join(workspace_dir, "evaluations.json")
    evaluations = {}

    if os.path.exists(evals_path):
        try:
            with open(evals_path, 'r', encoding='utf-8') as f:
                evaluations = json.load(f)
        except Exception:
            pass

    # Record the evaluation
    evaluations[stage] = f"{score}/100"

    # Save back
    try:
        with open(evals_path, 'w', encoding='utf-8') as f:
            json.dump(evaluations, f, ensure_ascii=False, indent=2)
        print(f"\n Successfully saved evaluation '{stage}': '{score}/100' to evaluations.json!")
    except Exception as e:
        print(f"Error saving to evaluations.json: {e}", file=sys.stderr)

    # 5. Export companion JS file for Local Dashboard to avoid CORS limits
    data_js_path = os.path.join(workspace_dir, "scripts", "data.js")
    js_content = (
        f"// Automatically generated by pr_reviewer.py. Do not edit manually.\n"
        f"const evaluationsData = {json.dumps(evaluations, ensure_ascii=False, indent=2)};\n\n"
        f"const latestReview = {json.dumps(review_data, ensure_ascii=False, indent=2)};\n"
    )
    try:
        os.makedirs(os.path.dirname(data_js_path), exist_ok=True)
        with open(data_js_path, 'w', encoding='utf-8') as f:
            f.write(js_content)
        print(" Updated local dashboard data (scripts/data.js) successfully!")
    except Exception as e:
        print(f"Error writing to dashboard data: {e}", file=sys.stderr)

    # 6. Telegram notifications & automatic chat-id resolution
    tg_token = os.environ.get("TELEGRAM_BOT_TOKEN")
    tg_chat_id = os.environ.get("TELEGRAM_CHAT_ID")

    if tg_token:
        if not tg_chat_id:
            # Resolve Chat ID dynamically
            tg_chat_id = resolve_telegram_chat_id(tg_token)
            if tg_chat_id:
                save_chat_id_to_env(tg_chat_id)
                # Reload env
                os.environ["TELEGRAM_CHAT_ID"] = str(tg_chat_id)
            else:
                print("\n[TELEGRAM INFO] To receive notifications in Telegram:")
                print("1. Search for your bot in Telegram and click 'Start' (send /start message).")
                print("2. Re-run this script to automatically resolve and save your Chat ID!")
        
        if tg_chat_id:
            send_telegram_message(tg_token, tg_chat_id, review_data)

if __name__ == "__main__":
    asyncio.run(main())
