#!/usr/bin/env python3
import os
# Bypasses local proxy routing for loopback websocket connections of the SDK
os.environ["no_proxy"] = "localhost,127.0.0.1"

import asyncio
import sys
import subprocess
import json
import pydantic
import requests

# Enable logging to show SDK activity and triggers
import logging
logging.basicConfig(level=logging.INFO)


try:
    from google.antigravity import Agent, LocalAgentConfig
    from google.antigravity.triggers import TriggerContext
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

SYSTEM_INSTRUCTIONS = """You are an automated Pull Request Auditor Daemon. Your role is to automatically review incoming code changes, identify bugs, and record scores.

When triggered:
1. Run git diff against origin/dev to see incoming PR changes.
2. Determine the stage name, summary, list of issues, and recommendations.
3. Assign a score from 0 to 100.
4. Output your full review matching the PullRequestReview schema."""

# Background Git Monitor Trigger
async def git_pr_poll_trigger(ctx: TriggerContext):
    """Proactive background trigger that monitors git branch modifications."""
    logging.info("[DAEMON] Git PR Poller started. Monitoring for branch and commit updates...")
    
    last_commit_sha = ""
    
    while True:
        try:
            # Silently fetch origin changes to stay up to date
            subprocess.run(["git", "fetch", "origin"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            
            # Check local HEAD commit
            res = subprocess.run(["git", "rev-parse", "HEAD"], capture_output=True, text=True, check=True)
            current_commit = res.stdout.strip()
            
            # Detect change in current branch state
            if last_commit_sha and current_commit != last_commit_sha:
                logging.info(f"[DAEMON] Event detected! New commit {current_commit[:8]} found. Triggering automated PR Code-Review...")
                
                # Extract git diff
                diff_content = get_git_diff()
                if not diff_content.strip():
                    diff_content = "[No active diff found. Evaluating existing files on latest commit.]"
                
                # Wake up the agent by sending an event message with the diff
                await ctx.send(
                    f"EVENT: New commit {current_commit[:8]} detected.\n\n"
                    f"Please perform a complete code review of the following pull request git diff:\n\n"
                    f"```diff\n{diff_content}\n```\n\n"
                    "Analyze the changes, identify potential bugs or issues, determine the stage name, "
                    "and assign an evaluation score out of 100."
                )
                
            last_commit_sha = current_commit
        except Exception as e:
            logging.error(f"[DAEMON] Error checking git updates: {e}")
            
        # Poll every 20 seconds
        await asyncio.sleep(20)

def get_git_diff():
    try:
        result = subprocess.run(
            ["git", "diff", "origin/dev...HEAD"],
            capture_output=True, text=True, check=True
        )
        if result.stdout.strip():
            return result.stdout
    except Exception:
        pass

    try:
        result = subprocess.run(
            ["git", "diff", "dev...HEAD"],
            capture_output=True, text=True, check=True
        )
        if result.stdout.strip():
            return result.stdout
    except Exception:
        pass

    try:
        result = subprocess.run(
            ["git", "diff", "HEAD~1"],
            capture_output=True, text=True, check=True
        )
        return result.stdout
    except Exception:
        return ""

def resolve_telegram_chat_id(token):
    """Automatically queries Telegram getUpdates to find the user's Chat ID."""
    print("[DAEMON] Attempting to automatically resolve Telegram Chat ID...")
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
                    print(f"[DAEMON] Found active Telegram Chat ID: {chat_id} ({first_name})")
                    return chat_id
    except Exception as e:
        print(f"[DAEMON] Error checking Telegram updates: {e}", file=sys.stderr)
    return None

def save_chat_id_to_env(chat_id):
    """Appends the resolved Chat ID to the .env file."""
    env_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".env")
    try:
        with open(env_path, "a", encoding="utf-8") as f:
            f.write(f"\nTELEGRAM_CHAT_ID={chat_id}\n")
        print("[DAEMON] Saved TELEGRAM_CHAT_ID to .env file!")
    except Exception as e:
        print(f"[DAEMON] Error saving Chat ID to .env: {e}", file=sys.stderr)

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
        f"🔍 *DAEMON AUTOMATED CODE REVIEW*\n\n"
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
            print("[DAEMON] Sent Code Review report to Telegram!")
        else:
            print(f"[DAEMON] Failed to send Telegram message: {response.text}", file=sys.stderr)
    except Exception as e:
        print(f"[DAEMON] Error sending Telegram notification: {e}", file=sys.stderr)

async def post_turn_hook(data: str):
    """Processes the agent's turn response to update evaluations.json, data.js, and send Telegram notifications."""
    print("\n" + "="*50)
    print(" [DAEMON] Intercepted Agent Code Review Response!")
    print("="*50)
    
    try:
        review_data = json.loads(data)
    except Exception as e:
        print(f"[DAEMON] Error: Failed to parse response string as JSON: {e}", file=sys.stderr)
        return

    stage = review_data.get("stage_name", "Unknown Stage")
    score = review_data.get("score", 0)
    summary = review_data.get("summary", "")
    issues = review_data.get("bugs_and_issues", [])
    recs = review_data.get("recommendations", [])

    print(f"\n* **Stage**: {stage}")
    print(f"* **Score**: {score}/100")
    print(f"\n### Summary:\n{summary}")

    workspace_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

    # 1. Update evaluations.json
    evals_path = os.path.join(workspace_dir, "evaluations.json")
    evaluations = {}

    if os.path.exists(evals_path):
        try:
            with open(evals_path, 'r', encoding='utf-8') as f:
                evaluations = json.load(f)
        except Exception:
            pass

    evaluations[stage] = f"{score}/100"

    try:
        with open(evals_path, 'w', encoding='utf-8') as f:
            json.dump(evaluations, f, ensure_ascii=False, indent=2)
        print(f"[DAEMON] Successfully saved evaluation '{stage}' to evaluations.json!")
    except Exception as e:
        print(f"[DAEMON] Error saving to evaluations.json: {e}", file=sys.stderr)

    # 2. Update scripts/data.js
    data_js_path = os.path.join(workspace_dir, "scripts", "data.js")
    js_content = (
        f"// Automatically generated by pr_reviewer_daemon.py. Do not edit manually.\n"
        f"const evaluationsData = {json.dumps(evaluations, ensure_ascii=False, indent=2)};\n\n"
        f"const latestReview = {json.dumps(review_data, ensure_ascii=False, indent=2)};\n"
        f"const lastUpdated = \"{subprocess.check_output(['date']).decode('utf-8').strip()}\";\n"
    )
    try:
        with open(data_js_path, 'w', encoding='utf-8') as f:
            f.write(js_content)
        print("[DAEMON] Updated local dashboard data (scripts/data.js) successfully!")
    except Exception as e:
        print(f"[DAEMON] Error writing to dashboard data: {e}", file=sys.stderr)

    # 3. Handle Telegram Notification
    tg_token = os.environ.get("TELEGRAM_BOT_TOKEN")
    tg_chat_id = os.environ.get("TELEGRAM_CHAT_ID")

    if tg_token:
        if not tg_chat_id:
            tg_chat_id = resolve_telegram_chat_id(tg_token)
            if tg_chat_id:
                save_chat_id_to_env(tg_chat_id)
                os.environ["TELEGRAM_CHAT_ID"] = str(tg_chat_id)
        
        if tg_chat_id:
            send_telegram_message(tg_token, tg_chat_id, review_data)
        else:
            print("\n[DAEMON TELEGRAM INFO] Send /start to your bot so it can resolve your Chat ID!")

    print("\n" + "="*50)

async def main():
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("Error: GEMINI_API_KEY environment variable is not set.", file=sys.stderr)
        sys.exit(1)

    workspace_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    
    # Configure the proactive daemon agent
    config = LocalAgentConfig(
        model="gemini-2.5-flash",
        system_instructions=SYSTEM_INSTRUCTIONS,
        workspaces=[workspace_dir],
        response_schema=PullRequestReview,
        triggers=[git_pr_poll_trigger],
        hooks=[post_turn_hook],
    )

    print("Starting Proactive PR Reviewer Daemon...")
    print("Press Ctrl+C to terminate.")
    
    async with Agent(config=config) as agent:
        # We start the agent session. Triggers run in the background.
        while True:
            await asyncio.sleep(1)

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nDaemon stopped.")
