# 0008: Auction end window of one hour to thirty days

Status: Accepted

## Context
The kata does not say how far in the future an auction may end. Too short an auction gives buyers no time to bid. Too long an auction holds a car for months and makes the seller's data stale.

## Decision
- `endsAt` must be at least 1 hour and at most 30 days after the injected `Clock`'s now. Both bounds are inclusive.
- The bounds are configurable: `bidanddrive.auction.min-duration` (default `PT1H`) and `bidanddrive.auction.max-duration` (default `P30D`).
- Violations are a 400 with an error on `endsAt`.

## Consequences
- The rule lives in the domain (`AuctionWindow`) and is tested with a fixed clock.
- Changing the bounds needs no code change. Specs 011 and 012 may rely on the same window.
