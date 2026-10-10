# 003: Identity and user switcher

Branch: `feat/003-identity-user-switcher`  
Status: draft

## User story
> As a visitor, I can pick which seeded user I am from a switcher, so that I can act as a seller or buyer without signing up.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given the database is migrated, when the app starts, then four seeded users exist with ids 1 to 4 and display names "Alice", "Bob", "Chloe" and "Dan".
2. Given a request to a protected endpoint with `X-User-Id: 2`, when it is processed, then it runs as user 2.
3. Given a request to a protected endpoint without `X-User-Id`, when it is processed, then the response is 401 with a ProblemDetail body.
4. Given a request with an `X-User-Id` that matches no user, when it is processed, then the response is 401.
5. Given `GET /api/users` is called without a header, when it runs, then it returns 200 with `{id, displayName}` for each seeded user.
6. Given the user picks "Bob" in the switcher, when the app makes any API call, then the header `X-User-Id: 2` is sent and the choice survives a page reload.
7. Given no choice has been stored, when the app loads, then user 1 is selected by default.

## Domain rules and edge cases
- Users are fixed seed data. There is no creation, login, password or session.
- Every user can be both seller and buyer. Roles are per auction (see 004 and 007).
- The domain receives a `UserId` value. Header parsing lives in the web adapter only.
- A stored user id that no longer exists is treated as "no choice" and falls back to user 1.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| GET | /api/users | none | 200 `[{id, displayName}]` | none |

Protected endpoints in later specs reject a missing or unknown `X-User-Id` with 401.

## UI (frontend)
- Shared component `src/components/UserSwitcher.vue`, placed in the app header.
- Store `src/features/identity/stores/identity.ts` (Pinia): `currentUserId`, `setUser(id)`, persisted to `localStorage` inside try/catch.
- API client `src/api/client.ts`: a fetch wrapper that adds `X-User-Id` from the identity store. Later specs use it for all calls.
- No new route.

## Slices
1. Backend: Flyway V2 creates the `app_user` table and seeds four users. `UserId` value type and `GET /api/users`. Covers criteria: 1, 5.
2. Backend: `CurrentUser` argument resolver reads `X-User-Id`, returns 401 ProblemDetail for missing or unknown users. Covers criteria: 2, 3, 4.
3. Frontend: identity store, API client header, `UserSwitcher` in the header. Covers criteria: 6, 7.

## Decisions
- Identity is a seeded user plus a trusted header. See [ADR 0001](../adr/0001-identity-user-switcher.md).

## Out of scope
- Login, passwords, sessions, tokens, user creation, profile editing.
