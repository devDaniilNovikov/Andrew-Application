#!/usr/bin/env python3
import os
import requests
import sys
import time

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

api_key = os.environ.get("GEMINI_API_KEY")
print(f"Checking Gemini API Connection with POST request...")
print(f"API Key: {api_key[:10]}...{api_key[-5:] if api_key else ''}")

# Let's try sending a POST request to streamGenerateContent
url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
params = {"key": api_key}
payload = {
    "contents": [{"parts": [{"text": "Hello"}]}]
}

print(f"\nSending POST request to {url}...")
start_time = time.time()
try:
    response = requests.post(url, params=params, json=payload, timeout=15)
    latency = time.time() - start_time
    print(f"-> Status Code: {response.status_code}")
    print(f"-> Latency: {latency:.2f} seconds")
    print(f"-> Response Body: {response.text[:500]}")
except requests.exceptions.Timeout:
    print(f"-> Timeout! Connection took more than 15 seconds.")
except requests.exceptions.RequestException as e:
    print(f"-> Request Exception: {e}")
