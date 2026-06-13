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

SYSTEM_INSTRUCTIONS = """You are a Jetpack Compose frontend engineer specializing in Google's Material Design 3. Your goal is to design high-fidelity, clean, and premium Jetpack Compose components based on UI specifications in PRD.md.

When asked to generate a component:
1. View PRD.md and locate any UI details or features related to the requested component.
2. Read section "Требования к интерфейсу" (Interface Requirements) and specific feature details to understand the layout and colors.
3. Generate complete, premium Kotlin Jetpack Compose code for that component.
4. The generated component MUST follow modern Jetpack Compose standards:
   - Use Material Design 3 (androidx.compose.material3.*).
   - Use standard Compose modifiers, layout components (Box, Row, Column, LazyColumn), states, and themes.
   - Include a fully fleshed out @Preview function with a mock theme/state so that the component can be rendered easily.
   - Include comments explaining key states, event lambdas (hoisting state), and styling decisions.
5. Provide a professional output detailing the structure and code block."""

async def main():
    if len(sys.argv) < 2:
        print("Usage: python3 generate_ui_components.py <Component_Name>", file=sys.stderr)
        print("Example: python3 generate_ui_components.py ActiveRequestCard", file=sys.stderr)
        sys.exit(1)

    component_name = sys.argv[1]

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

    print(f"Initializing Jetpack Compose Expert Agent for component '{component_name}'...")
    async with Agent(config=config) as agent:
        response = await agent.chat(
            f"Please search PRD.md for the UI specs related to '{component_name}'. "
            f"Then generate premium, clean Jetpack Compose code with @Preview and full Material 3 compliance for it."
        )
        
        print(f"\n=== GENERATED {component_name.upper()} COMPONENT ===\n")
        async for token in response:
            print(token, end="", flush=True)
        print("\n\n=== END OF COMPONENT ===\n")

if __name__ == "__main__":
    asyncio.run(main())
