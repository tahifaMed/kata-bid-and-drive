# 003: Identity and user switcher

Branch: `feat/003-identity-user-switcher`  
Status: implemented, pending review

## User story
> As a visitor, I can pick which seeded user I am from a switcher, so that I can act as a seller or buyer without signing up.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given the database is migrated, when the app starts, then exactly four seeded users exist with ids 1 to 4 and display names "Alice", "Bob", "Chloe" and "Dan".
2. Given a request to a `@CurrentUser` parameter with `X-User-Id: 2`, when it is processed, then it runs as user 2. No production endpoint in this spec takes a `@CurrentUser` parameter, so this is tested through a test-only probe controller. Real protected endpoints arrive in 004 and later.
3. Given a request to a protected endpoint without `X-User-Id`, when it is processed, then the response is 401 with a ProblemDetail body (see the error contract below).
4. Given a request with an `X-User-Id` that matches no user, when it is processed, then the response is 401.
5. Given `GET /api/users` is called without a header, when it runs, then it returns 200 with a JSON array of exactly four objects, ordered by id: `[{"id":1,"displayName":"Alice"},{"id":2,"displayName":"Bob"},{"id":3,"displayName":"Chloe"},{"id":4,"displayName":"Dan"}]`.
6. Given the user picks "Bob" in the switcher, when the app makes any API call, then the header `X-User-Id: 2` is sent and the choice survives a page reload.
7. Given no choice has been stored, when the app loads, then user 1 is selected by default and sent as `X-User-Id: 1`.
8. Given a stored choice of 9 that matches no seeded user, when the app loads and the user list arrives, then user 1 is selected and the stored value is replaced with 1.

## Domain rules and edge cases
- Users are fixed seed data. There is no creation, login, password or session.
- Every user can be both seller and buyer. Roles are per auction (see 004 and 007).
- The domain receives a `UserId` value. Header parsing lives in the web adapter only.
- A header value is valid only when it is a positive decimal integer after trimming whitespace. Anything else (empty, non-numeric, `0`, negative, `1e3`) gives 401, never 400, and does not query the user list.
- A stored user id that matches no seeded user is reset to user 1 when the user list loads. Until that list loads, the stored id is sent as is, so a request made in that window can get 401. The switcher loads the list when it mounts, so the window is short.
- If the user list fails to load, the switcher shows "Users unavailable" and keeps the stored id. No reset happens.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| GET | /api/users | none | 200 `[{id, displayName}]` as in criterion 5 | none |

The response is a plain array, not HAL. See [ADR 0007](../adr/0007-plain-user-list.md).

Protected endpoints in later specs reject a missing or unknown `X-User-Id` with 401.

### Error contract (401)
The body is an RFC 9457 ProblemDetail with `status: 401`, `title: "Unauthorized"`, `type: "about:blank"` and `detail: "X-User-Id header is missing or does not match a known user"`. No stack trace and no other fields.

## UI (frontend)
- Shared component `src/components/UserSwitcher.vue`, placed in the app header. The header of the auctions page is the only app header today.
- `src/features/identity/stores/identity.ts` (Pinia store `identity`): `currentUserId`, `users`, `setUser(id)` and `loadUsers()`. The choice is stored in `localStorage` under `bidanddrive.currentUserId`, inside try/catch.
- `src/features/identity/api.ts`: `fetchUsers()`. `src/features/identity/types.ts`: `UserSummary {id, displayName}`.
- `src/api/client.ts`: `apiFetch(path, init)`, the only place that calls `fetch`. It adds `X-User-Id` from the identity store. A non-2xx response throws `ApiError`, which has a numeric `status`. Later specs use it for all calls and rely on `status` for 401 and 409 handling.
- No new route.

## Slices
1. Backend: Flyway V2 creates the `app_user` table and seeds four users. `UserId` value type and `GET /api/users`. Covers criteria: 1, 5. Landed as one commit.
2. Backend: `CurrentUser` argument resolver reads `X-User-Id`, returns 401 ProblemDetail for missing or unknown users. Covers criteria: 2, 3, 4.
3. Frontend: identity store, API client header, `UserSwitcher` in the header. Covers criteria: 6, 7, 8.

## Decisions
- Identity is a seeded user plus a trusted header. See [ADR 0001](../adr/0001-identity-user-switcher.md).
- `GET /api/users` returns a plain array with no `_links`. See [ADR 0007](../adr/0007-plain-user-list.md).
- A stored id that matches no user resets to 1 when the user list loads (criterion 8). This is decided here, not in an ADR.

## Out of scope
- Login, passwords, sessions, tokens, user creation, profile editing.
