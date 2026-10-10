# 0010: Create response is a subset of the detail shape

Status: Accepted

## Context
`POST /api/auctions` could return a minimal body, or the full detail from 006. A minimal body means the client calls `GET` again to show the auction. The full detail has bid history, which a new auction does not have yet.

## Decision
- The 201 body has the same field names as the 006 detail (`car`, `seller {displayName}`, `startingPrice`, `currentBid`, `minNextBid`, `bidCount`, `highestBidderId`, `bids`, `endsAt`, `status`, `_links`). A new auction has `bidCount: 0`, `bids: []` and `highestBidderId: null`.
- The `place-bid` link is included, because an OPEN auction always has it (006).
- Later specs may add fields to the body but not remove or rename these.

## Consequences
- One DTO serves 004 and 006, so the frontend type is shared.
- The 201 body has no field that 006 does not define.
