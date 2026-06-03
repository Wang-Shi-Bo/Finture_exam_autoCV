---
name: test-flow
description: Run full project validation — compile, test, check servers
---

# Test Flow

Run through the project's complete validation pipeline.

## Steps

1. **Compile** — `cd backend && mvn compile -q`
2. **Test** — `cd backend && mvn test -q`
3. **Server health** — check if backend is on `:8080` and frontend on `:5173`
4. **API check** — if backend is running, quick curl to verify `/api/resume/parse` responds

## Output

Report each step as pass/fail. If any step fails, suggest the fix.
