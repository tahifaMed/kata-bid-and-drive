---
name: test
description: Run the full backend and frontend test and lint suites for the current branch and report results honestly. Use before committing a slice's final state, before opening a PR, or when asked whether the build is green.
---

# Test

1. Backend: `cd backend && ./mvnw verify`
2. Frontend: `cd frontend && npm run build && npm run test:unit -- --run && npm run lint`
3. If anything fails, read the first real error, find the root cause, and fix the code. Do not delete, skip or weaken tests.
4. Report the exact commands run and their pass or fail result. If a step did not run, say so and why.
