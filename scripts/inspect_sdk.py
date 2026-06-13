#!/usr/bin/env python3
import os
import sys

try:
    from google.antigravity import LocalAgentConfig, AgentConfig, GeminiConfig
    import pydantic
    
    print("LocalAgentConfig fields:")
    for name, field in LocalAgentConfig.model_fields.items():
        print(f"- {name}: {field.annotation} (default: {field.default})")
        
    print("\nGeminiConfig fields:")
    for name, field in GeminiConfig.model_fields.items():
        print(f"- {name}: {field.annotation} (default: {field.default})")
except ImportError as e:
    print(f"Error: {e}")
