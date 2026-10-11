# 0001: Identity via seeded users and X-User-Id header

Status: Accepted

## Context
The kata has no authentication requirement, but bidding needs a buyer identity, "my bids" needs to know whose bids to show, and the demo needs several buyers bidding on the same car.

## Decision
- The database holds a fixed set of seeded users. There is no sign-up, login or password.
- The client sends the acting user's id in the `X-User-Id` header. A resolver turns it into a `UserId`. A missing or unknown id gives 401.
- The frontend stores the chosen user in `sessionStorage`, so each tab keeps its own user and two tabs can bid as two users. A reload keeps the tab's choice; a new tab starts as user 1. It sends the header on every call. A user switcher in the app shell changes it. (Changed from `localStorage`, which every tab shares, so live bidding between users could not be tried in one browser.)
- Every user can act as seller or buyer. Roles are per auction, not per user.

## Consequences
- The header is trusted. This is not secure and must not be deployed as is.
- Real authentication can replace the resolver later without changing domain code, since the domain only sees a `UserId`.
