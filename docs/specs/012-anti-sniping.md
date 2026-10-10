# 012: Anti-sniping (soft close)

Branch: `feat/012-anti-sniping`  
Status: draft

## User story
> As a buyer, I am not beaten by a bid placed in the last seconds of an auction, so that the auction ends fairly.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test. The window is 2 minutes unless configured otherwise.
1. Given auction 5 ends at T, when user 2 bids at T minus 3 minutes, then `endsAt` stays at T and the event has `extended: false`.
2. Given auction 5 ends at T, when user 2 bids at T minus 90 seconds, then `endsAt` becomes that bid's time plus 2 minutes, the response includes the new `endsAt`, and the `bid-placed` event has `extended: true` with the new `endsAt`.
3. Given auction 5 was extended and a bid arrives within the new window, when it is placed, then `endsAt` is extended again, so repeated last-second bids keep extending the auction.
4. Given a bid placed exactly at the boundary (`endsAt` minus window), when it is placed, then it does not extend the auction. Only bids strictly inside the window extend.
5. Given I am on the auction page and an event with a new `endsAt` arrives, when it is applied, then the countdown updates and a "Time extended" note is shown briefly.
6. Given the window is configured to `PT0S`, when a bid is placed in the last minute, then the auction is not extended.
7. Given two bids race inside the window, when both commit through the version check (007), then `endsAt` is set once per committed bid and no bid after the final `endsAt` is accepted.

## Domain rules and edge cases
- Rule: if `endsAt - placedAt < window` and `endsAt > placedAt`, set `endsAt = placedAt + window`. Otherwise leave it. The new `endsAt` is never earlier than the old one.
- Extension applies only to `OPEN` auctions.
- Time uses the injected `Clock` for `placedAt`, not a client timestamp.
- No cap on total extensions. This is a kata choice, recorded below.
- The window is a configuration property `auction.anti-sniping.window` (ISO-8601 duration), default `PT2M`.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| POST | /api/auctions/{id}/bids | unchanged | 201 bid, adds `endsAt` | (unchanged) |

The `bid-placed` event (009) gains `extended` and the new `endsAt`. No new event type.

## UI (frontend)
- `AuctionDetailView` countdown reads `endsAt` from each event (010).
- A short "Time extended" note appears for 5 seconds after an event with `extended: true`. Component `src/features/auction/components/ExtendedNotice.vue`.

## Slices
1. Domain: extension rule, boundary handling, configuration property with default `PT2M`. Unit tests with a fixed `Clock`. Covers criteria: 1, 2, 4, 6.
2. Backend: apply the extension in the bid transaction, return `endsAt` and `extended` in the response and event. Integration tests for repeated extension and the race. Covers criteria: 2, 3, 7.
3. Frontend: countdown from `endsAt`, `ExtendedNotice`. Covers criteria: 5.

## Decisions
- No cap on extensions. An unlimited soft close can run long; a cap is a later spec if needed.
- The extended state is an additive field on the existing event rather than a separate event type, to keep the event contract small (see 009).

## Out of scope
- Proxy bidding, per-auction windows, extension notification emails.
