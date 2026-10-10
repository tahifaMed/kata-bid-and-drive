# 0005: HTTP status mapping and check order for bid errors

Status: Accepted

## Context
A bid can fail for several reasons. The API must return one consistent status per cause, and when several rules apply, the answer must be predictable.

## Decision
Status codes for the bid endpoint:
- **400**: the request body is malformed or the amount is invalid (format, scale, sign, JSON type).
- **401**: no identity, or an unknown identity ([ADR 0001](0001-identity-user-switcher.md)).
- **403**: the caller is not allowed to bid on this auction (seller of the auction).
- **404**: the auction does not exist.
- **409**: a state conflict. Auction closed, caller already highest bidder, or a version conflict on commit.
- **422**: the amount is valid but below the minimum bid. The ProblemDetail carries `minimumBid`.

Check order, first failure wins:
1. identity (401)
2. auction exists (404)
3. body valid (400)
4. seller (403)
5. closed (409)
6. already highest (409)
7. minimum (422)
8. version conflict at commit (409)

## Consequences
- A client can tell "raise your bid" (422) from "stop, the state changed" (409) by status alone.
- Closed is checked before the minimum, so a late bid on a closed auction is always reported as closed.
- Any new bid rule must state its status and its place in this order.
