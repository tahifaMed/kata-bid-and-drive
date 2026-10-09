---
name: open-pr
description: Push the current feature branch and open a GitHub pull request linked to its spec, with a short body and test evidence. Use when all slices of a spec are done and /test passes.
disable-model-invocation: true
---

# Open a PR

1. Confirm `/test` passed on the current state. If it did not, stop and say so.
2. Confirm the branch is `<type>/<NNN>-<slug>` and based on an up-to-date `main`. Rebase on `main` if needed. Never force-push.
3. Push with `git push -u origin <branch>`.
4. Create the PR with `gh pr create`:
   - Title: `type(scope): summary (#NNN)`, matching the main commit.
   - Body:
     - Spec: link to `docs/specs/NNN-slug.md`
     - Changes: up to 3 bullets
     - Test evidence: the commands run and their results
     - Open risks or decisions, if any
   - End the body with: `🤖 Generated with [Claude Code](https://claude.com/claude-code)`
5. Print the PR URL. Stop for human review.
