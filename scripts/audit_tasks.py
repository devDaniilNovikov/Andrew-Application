#!/usr/bin/env python3
import asyncio
import os
import sys

# Enable logging to show SDK activity
import logging
logging.basicConfig(level=logging.WARNING)

try:
    from google.antigravity import Agent, LocalAgentConfig
except ImportError:
    print("Error: google-antigravity package is not installed. Please install it using pip.", file=sys.stderr)
    sys.exit(1)

SYSTEM_INSTRUCTIONS = """You are a software Project Manager Agent. Your role is to audit the TASKS.md file against the actual code and files present in this repository, and then update TASKS.md with correct completion checkmarks.

When asked to audit tasks:
1. Locate and read the `TASKS.md` file. Find all checkbox groups (e.g. Stage 0, Stage 1, Stage 2, etc.) and checkmarks like `[ ]`, `[/]`, `[x]`.
2. Scan the project workspace directories to check if the folders or files specified in each checklist item exist and have been implemented. For example:
   - Stage 0: Main entry point MainActivity, dependencies, basic settings, and packages.
   - Stage 1: Room entities (Request.kt), DAOs (RequestDao.kt), database, and converters.
   - Stage 2: Navigation and screen layout files.
   - Stage 3-12: Form screens, active lists, bottom sheets, alarm managers, and contact importing.
3. Update the checkmarks directly in the file TASKS.md:
   - Use `[x]` if the files and implementation are fully complete and present in the codebase.
   - Use `[/]` if the files or stages are partially implemented or in-progress.
   - Use `[ ]` if they have not been created or started yet.
4. Modify the `TASKS.md` file directly using your edit_file or create_file tool. Make sure to preserve all markdown text, formatting, Russian descriptions, and stage details exactly as they are—ONLY modify the checkmark characters inside `[ ]`, `[/]`, or `[x]`.
5. After updating the file, print a clear summary of which tasks were updated."""

async def main():
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("Error: GEMINI_API_KEY environment variable is not set.", file=sys.stderr)
        print("Please export your API key: export GEMINI_API_KEY='your-key-here'", file=sys.stderr)
        sys.exit(1)

    workspace_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    
    config = LocalAgentConfig(
        system_instructions=SYSTEM_INSTRUCTIONS,
        workspaces=[workspace_dir],
    )

    print("Initializing Task Auditor Agent...")
    async with Agent(config=config) as agent:
        print("Auditing TASKS.md against the actual repository codebase status...")
        response = await agent.chat(
            "Please audit the TASKS.md file against the actual files in this repository workspace, "
            "determine which tasks are completed, in progress, or unstarted, and update TASKS.md with "
            "the corresponding checkmarks. Then print a brief summary of the changes you made."
        )
        
        print("\n=== AUDIT PROCESS OUTPUT ===\n")
        async for token in response:
            print(token, end="", flush=True)
        print("\n\n=== END OF AUDIT ===\n")

if __name__ == "__main__":
    asyncio.run(main())
