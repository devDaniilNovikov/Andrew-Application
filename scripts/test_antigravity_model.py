#!/usr/bin/env python3
import os
os.environ["no_proxy"] = "localhost,127.0.0.1"

import asyncio
import sys

try:
    from google.antigravity import Agent, LocalAgentConfig
except ImportError:
    print("Error: google-antigravity not installed.", file=sys.stderr)
    sys.exit(1)

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

async def main():
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("GEMINI_API_KEY not set.")
        sys.exit(1)

    workspace_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    
    print("Initializing test agent with model='gemini-3.1-pro-preview'...")
    config = LocalAgentConfig(
        model="gemini-3.1-pro-preview",
        system_instructions="You are a helpful assistant.",
        workspaces=[workspace_dir],
    )

    async with Agent(config=config) as agent:
        print("Sending message...")
        response = await agent.chat("Say 'Antigravity is online!' in Russian.")
        print(f"Response: {response.text}")

if __name__ == "__main__":
    asyncio.run(main())
