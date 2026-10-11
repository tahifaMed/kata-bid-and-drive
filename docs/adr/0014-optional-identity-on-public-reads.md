# 0014: Optional identity on public reads

Status: Accepted

## Context
008 adds `myBid` to `GET /api/auctions/{id}`. The detail is public (006), and a test pins that an unknown or malformed `X-User-Id` still gets 200. Endpoints that need identity (`POST` bids, the single-bid `GET`, `/api/me/*`) answer 401 for a missing or unknown id (ADR 0001, ADR 0013).

## Decision
- On a public read, identity is optional. A missing, malformed or unknown `X-User-Id` is treated as anonymous: no 401, and per-user fields such as `myBid` are left out.
- Endpoints that need identity keep the 401 rule.
- The web layer marks an optional identity with `@CurrentUser(required = false) UserId`, which is null for an anonymous caller. The default stays required. A malformed id is rejected before the user list is queried (003).

## Consequences
- Earlier clients and the existing public-detail tests are unaffected.
- A client with a stale user id sees the public detail instead of an error. It notices only when it calls an endpoint that needs identity.
- 013 (`watched` on the detail) can reuse the same rule.
