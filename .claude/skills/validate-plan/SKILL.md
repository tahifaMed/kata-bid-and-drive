---
name: validate-plan
description: Challenge a draft spec with the plan-critic subagent before any code is written. Use after plan-feature, or whenever a spec's status is draft.
argument-hint: "<path to docs/specs/NNN-slug.md>"
disable-model-invocation: true
---

# Validate a plan

1. Run the `plan-critic` subagent on the spec path. It returns findings grouped as blocker, risk, or nit.
2. Fix blockers and risks in the spec itself. Do not just reply in chat.
3. Re-run `plan-critic` if you changed acceptance criteria or slices.
4. When there are no blockers, set Status to `validated` and tell the human the verdict: go or no-go, with the remaining risks.
5. Stop for the human's go-ahead. Do not start implementation on your own.
