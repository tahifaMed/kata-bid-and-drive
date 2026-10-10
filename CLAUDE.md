# BidAndDrive

Car auction kata. Sellers put cars up for auction; buyers bid, and other buyers can bid on the same car.
Spec-first: every feature starts as a numbered spec in `docs/specs/`. The spec is the source of truth.

## Stack
- Backend: Java 25, Spring Boot 4.0.x (`backend/`, see `backend/CLAUDE.md`)
- Frontend: Vue 3 + TypeScript + Vite + Pinia + Vue Router (`frontend/`, see `frontend/CLAUDE.md`)
- Persistence: PostgreSQL, Flyway migrations
- Packaging and CI/CD: not in scope yet

## Commands
- Backend: `cd backend && ./mvnw verify`
- Frontend: `cd frontend && npm run build && npm run test:unit && npm run lint`

## Workflow (mandatory for any feature)
`/feature <story>` runs the pipeline: plan → validate → develop → test → commit → PR → review.
1. `/plan-feature` writes `docs/specs/NNN-slug.md` (acceptance criteria, API, domain rules, slices). Stop for human approval.
2. `/validate-plan` runs the `plan-critic` subagent against the spec. Stop for human go/no-go.
3. `/implement-slice` per slice, TDD: failing test → minimal code → green → refactor.
4. `/commit` per logical change. `/open-pr` when the spec's slices are done and tests pass.
5. `/review-pr` runs `code-reviewer` in a fresh context. Address findings, then merge.

Procedures live in skills, not here. Read the matching skill before doing that kind of work.

## Git rules
- One feature = one numbered spec = one branch = one PR.
- Branch: `<type>/<NNN>-<slug>`, for example `feat/001-create-auction`, `fix/003-bid-race`. Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.
- Never commit or push to `main`. Branch from an up-to-date `main`.
- Never force-push. Never use `--no-verify`.
- Commit subject: `type(scope): imperative summary`, lowercase, no trailing period, at most 60 chars. Example: `feat(auction): add create auction endpoint`.
- Body only when the why is not obvious, at most 2 short lines. No file lists, no emoji.
- Keep commits small. Tests go in the same commit as the code they cover.
- PR title: `type(scope): summary (#NNN)`. Body: spec link, up to 3 bullets of changes, test evidence.

## Architecture
- Backend: domain logic does not depend on Spring or JPA. Controllers are thin. Money uses `BigDecimal`, time uses `Instant` plus an injected `Clock`.
- Frontend: feature folders under `src/features/<feature>/`. Shared UI under `src/components/`.
- The API is hypermedia-style (HATEOAS) and documented with OpenAPI, once the API features land.

## Decisions
When the kata is ambiguous, pick an option, record it as an ADR in `docs/adr/NNNN-title.md`, and stick to it.

## Definition of done
- The spec's acceptance criteria are each covered by a test.
- `./mvnw verify` and the frontend build, unit tests and lint pass.
- Commits follow the git rules. The PR is open and linked to the spec.
