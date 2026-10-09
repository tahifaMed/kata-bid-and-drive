---
name: review-pr
description: Review the current branch's diff against main with a fresh-context code-reviewer subagent, then report verified findings. Use after open-pr, or before asking a human to merge.
argument-hint: "[PR number]"
disable-model-invocation: true
---

# Review a PR

1. Collect the diff: `git diff main...HEAD`, plus the linked spec's acceptance criteria.
2. Run the `code-reviewer` subagent with only the diff and the criteria. It must not see the implementation conversation.
3. Verify each finding against the code before reporting it. Drop findings that do not hold.
4. Report findings ranked by severity: correctness and security first, then tests, then style.
5. If findings need fixes, fix them in new small commits with `/commit`, then run `/test` again. Do not amend pushed commits.
