---
name: plan-feature
description: Turn a user story into a numbered spec in docs/specs with acceptance criteria, API, domain rules and slices. Use when starting any new feature or story, before writing code.
argument-hint: "<story text> [NNN-slug]"
disable-model-invocation: true
---

# Plan a feature

1. Pick the next free number: look at `docs/specs/` and take max + 1, zero-padded to 3 digits. Slug is kebab-case from the title.
2. Read `CLAUDE.md`, `backend/CLAUDE.md` and `frontend/CLAUDE.md`. Explore only the code this story touches.
3. Copy `docs/specs/_template.md` to `docs/specs/NNN-slug.md` and fill every section:
   - Acceptance criteria in Given/When/Then. The kata has none, so write them and mark each assumption.
   - Domain rules and edge cases: concurrency, invalid input, unauthorized role, closed auction.
   - API and UI changes.
   - Slices: small, each one a commit, each with the criteria it covers.
4. Record any choice the kata left open in the spec's Decisions section. Create an ADR in `docs/adr/` only for decisions that affect architecture.
5. Set Status to `draft`. Stop and show the human the spec path and a summary. Do not write code.
