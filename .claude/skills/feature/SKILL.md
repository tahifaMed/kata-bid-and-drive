---
name: feature
description: Run the whole feature pipeline for one user story, plan → validate → develop → test → commit → PR → review, stopping at the two human gates. Use when the user asks to build a story end to end.
argument-hint: "<story text>"
disable-model-invocation: true
---

# Feature pipeline

Run the steps in order. Stop only where marked.

1. `/plan-feature <story>`: creates the spec, status draft.
2. `/validate-plan`: critic review and fixes.
3. **Human gate A**: show the spec summary and wait for approval.
4. Create the branch `feat/NNN-slug` from an up-to-date `main`.
5. `/implement-slice` for each slice, with `/commit` after each.
6. `/test`.
7. `/open-pr`.
8. `/review-pr`.
9. **Human gate B**: show the PR URL and review result. The human merges.

Never skip a step. If a step fails, fix it or stop and report what is blocked.
