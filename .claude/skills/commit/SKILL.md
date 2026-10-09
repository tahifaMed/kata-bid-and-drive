---
name: commit
description: Create one small, clear Conventional Commit for the staged logical change on a feature branch. Use after finishing a slice or a logical change, never on main.
disable-model-invocation: true
---

# Commit

1. Check the branch: `git branch --show-current` must be `<type>/<NNN>-<slug>`. Never commit on `main`.
2. Review `git status` and `git diff --staged`. If the change mixes concerns, unstage and split it into separate commits.
3. Stage only the files of this logical change. Tests go with the code they cover.
4. Message format:
   - Subject: `type(scope): imperative summary`, lowercase, no trailing period, at most 60 chars. Example: `feat(auction): add create auction endpoint`.
   - Body: only if the why is not obvious, at most 2 short lines. No file lists.
   - Types: feat, fix, refactor, test, docs, chore.
5. Commit with a heredoc so the body formats correctly. End with the co-author trailer:
   `Co-Authored-By: Claude Haiku 5.5 <noreply@anthropic.com>`
6. Never use `--no-verify`. If a hook blocks the commit, fix the message or the change.
