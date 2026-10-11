---
name: implement-slice
description: Implement one slice of a validated spec with TDD, one branch per spec. Use when a spec has status validated or in-progress and a slice is next.
argument-hint: "<NNN> [slice number]"
---

# Implement a slice

Preconditions: the spec is `validated`, and you are on branch `<type>/<NNN>-<slug>`. If not, create it from an up-to-date `main` (`git switch -c feat/NNN-slug`). Never work on `main`.

For each slice, in order:
1. Read the slice and the acceptance criteria it covers.
2. Write a failing test for the behaviour. Run it and confirm it fails for the right reason.
3. Write the smallest code that makes it pass.
4. Run the affected test set: `./mvnw -Dtest=... test` in `backend/`, or `npm run test:unit -- <path>` in `frontend/`.
5. Refactor while green. Keep domain code free of Spring and JPA.
6. Run the full checks for the touched module. Fix failures at the root cause. Never weaken or delete a test to get green.
7. Commit with `/commit`. One slice is one logical commit, tests included.
8. Mark the slice done in the spec and move on.

When all slices are done, run `/test`, then `/open-pr`, then `/review-pr`. Stop at human gate B: show the PR URL and the review result.
