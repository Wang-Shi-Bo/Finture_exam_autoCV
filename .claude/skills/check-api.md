---
name: check-api
description: Verify DeepSeek API key validity and connectivity
---

# Check API

Verify that the DeepSeek API key is correctly configured and the service is reachable.

## Steps

1. **Find the key** — read from `DEEPSEEK_API_KEY` env var, or `backend/src/main/resources/application-local.yml`
2. **Test connectivity** — `curl` to `https://api.deepseek.com/v1/chat/completions` with a minimal request (1 token)
3. **Report** — valid/invalid, model name, any rate limits or error codes

If the key is invalid or expired, tell the user to go to https://platform.deepseek.com to get a new one.
