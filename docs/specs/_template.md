# NNN: <feature title>

Branch: `feat/NNN-<slug>`  
Status: draft | validated | in-progress | done

## User story
> As a <role>, I can <action>, so that <benefit>.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given ..., when ..., then ...

## Domain rules and edge cases
- Rule (for example: a bid must be strictly higher than the current highest bid)
- Edge case (for example: concurrent bids on the same car)

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|

## UI (frontend)
Routes, components, store changes.

## Slices
Each slice is one commit (or a small series). Tests live with the code.
1. Slice title. Covers criteria: 1, 2
2. ...

## Decisions
Choices made where the kata was silent. Link ADRs in `docs/adr/`.

## Out of scope
