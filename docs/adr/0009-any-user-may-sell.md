# 0009: Any user may sell

Status: Accepted

## Context
The kata does not say who may put a car up for auction. A seller role would need a way to grant it, and identity is a fixed list of seeded users (see [ADR 0001](0001-identity-user-switcher.md)).

## Decision
- Any user identified by `X-User-Id` may create an auction. The seller is always the caller.
- No seller role is stored.

## Consequences
- A user can sell and bid. The bidding rules in [ADR 0006](0006-bidding-rules.md) stop a seller from bidding on their own auction.
- Adding a role later means a new ADR and a migration.
