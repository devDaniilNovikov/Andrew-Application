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

SYSTEM_INSTRUCTIONS = """You are a meticulous Database Auditor Agent. Your responsibility is to audit the Room database models and schemas in this Android repository against the requirements defined in PRD.md.

When asked to verify the schema:
1. Locate the file PRD.md using standard directory searches or file views. Read its "Модель данных" (Data Model) section carefully to understand the required fields, datatypes, and constraints for the `Request` entity and database.
2. Search the repository to check if there are any Kotlin Room entities, DAOs, converters, or databases defined (typically under app/src/main/java or a data directory).
3. If no files exist yet, provide a detailed summary of what files need to be created, what exact fields each model must have, what their SQLite / Kotlin data types should be (including Nullability according to the PRD), and how Room TypeConverters should map them (e.g. status enums, Date/Long nextActionDateTime).
4. If Kotlin files DO exist, compare their fields, nullability, primary keys, indexing, and DAO query/sorting methods against the PRD. Specifically verify:
   - All 17 fields in the `Request` model are present and have the exact matching types.
   - Sorter in Active DAO: Sorted by `nextActionDateTime ASC` (earliest first).
   - Sorter in History DAO: Sorted by `closedAt DESC` (newest closed first) or optionally grouped by status first, then closedAt DESC.
5. Produce a professional, markdown-formatted audit report summarizing the alignment status, pointing out any discrepancies or listing instructions for implementing them correctly."""

async def main():
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("Error: GEMINI_API_KEY environment variable is not set.", file=sys.stderr)
        print("Please export your API key: export GEMINI_API_KEY='your-key-here'", file=sys.stderr)
        sys.exit(1)

    # We set workspaces to the current workspace root directory
    workspace_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    
    config = LocalAgentConfig(
        system_instructions=SYSTEM_INSTRUCTIONS,
        workspaces=[workspace_dir],
    )

    print("Initializing Database Auditor Agent...")
    async with Agent(config=config) as agent:
        print("Auditing codebase against PRD schema...")
        response = await agent.chat(
            "Verify the codebase's database schema and Kotlin files against the PRD.md requirements. "
            "Please view the PRD.md file and search for any Kotlin entity or DAO files in the project. "
            "Provide a comprehensive, professional schema audit report."
        )
        
        print("\n=== SCHEMA AUDIT REPORT ===\n")
        async for token in response:
            print(token, end="", flush=True)
        print("\n\n=== END OF REPORT ===\n")

if __name__ == "__main__":
    asyncio.run(main())
