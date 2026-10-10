# 011: Reserve price and auto-close

Branch: `feat/011-reserve-and-auto-close`  
Status: draft

## User story
> As a seller, I can set a hidden reserve price and have each auction close automatically at its end time, so that every auction ends with a clear result.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given the create form has `reserve: "5000.00"` and `startingPrice: "1000.00"`, when the auction is posted, then the response is 201 and the reserve is stored, and no response (create, detail or list) ever contains the reserve value.
2. Given `reserve` is less than `startingPrice`, when the auction is posted, then the response is 400.
3. Given auction 5 has ended with highest bid "4000.00" and reserve "5000.00", when the closer runs, then the auction becomes `CLOSED` with `outcome: "RESERVE_NOT_MET"`, `winnerId: null`, and an `auction-closed` event is published.
4. Given auction 6 has ended with highest bid "5200.00" by user 3 and reserve "5000.00" (or no reserve), when the closer runs, then the auction becomes `CLOSED` with `outcome: "SOLD"`, `winnerId: 3`, and `currentBid` is the final price.
5. Given auction 7 has ended with no bids, when the closer runs, then the outcome is `NO_BIDS`.
6. Given a closed auction, when a bid is posted, then the response is 409 (closed), as in 007.
7. Given the closer runs twice, or runs while a bid is being placed, when they finish, then each auction is closed exactly once and no bid after `endsAt` is accepted.
8. Given user 3 won auction 6 and user 2 bid on auction 5 (reserve not met), when `GET /api/me/bids` is called, then user 3 sees `WON` for auction 6 and user 2 sees `LOST` for auction 5. Existing statuses are unchanged for open auctions.
9. Given a closed auction's page, when it loads, then a result banner shows "Sold to B*** for 5200.00", "No bids" or "Reserve not met", and the bid panel is hidden.

## Domain rules and edge cases
- Auction status is `OPEN` or `CLOSED`. Outcome is `SOLD`, `RESERVE_NOT_MET` or `NO_BIDS`, set only when closed. Precedence: no bids, then reserve not met, then sold.
- The closer closes an auction only when `status = OPEN` and `endsAt <= now` (injected `Clock`), in batches of at most 100.
- The closer uses the optimistic `@Version` check from 007, so it cannot overwrite a bid that was committed first ([ADR 0003](../adr/0003-optimistic-locking-for-bids.md)).
- The reserve is never exposed to any API client, including the seller. This is a kata choice, recorded below.
- Ended auctions are not in the open list (005 already filters by `endsAt`).
- `auction-closed` envelope data: `{outcome, winnerId, finalPrice}`, with `finalPrice` null when there are no bids.
- The closer is a scheduled job, runs every 15 seconds (configurable), and lives in the adapter layer. The use case `CloseDueAuctions` is plain application code and takes a `Clock`.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| POST | /api/auctions | adds optional `reserve` (string, scale 2) | 201 (unchanged shape, no reserve) | 400 (reserve below starting price) |
| GET | /api/auctions/{id} | none | adds `outcome` and `winnerId`, `status` can be `CLOSED` | (unchanged) |
| GET | /api/me/bids | none | `status` can also be `WON` or `LOST` | (unchanged) |

Event `auction-closed` on `GET /api/auctions/{id}/events` (009).

## UI (frontend)
- `AuctionForm` (004) gains an optional "Reserve price" field with the help text "Hidden from buyers. The auction closes below this price with no sale."
- `AuctionResultBanner.vue` in `src/features/auction/components/`, shown on closed auctions.
- `AuctionDetailView` (006) hides the bid panel when `status` is `CLOSED`, and handles `auction-closed` events through `useAuctionStream` (010).

## Slices
1. Domain: `reserve` field, outcome rule and precedence, close rule with `Clock`. Unit tests. Covers criteria: 4, 5 (rules).
2. Backend: Flyway V5 adds `reserve_price`, `outcome`, `winner_id`, `closed_at`. Scheduled closer, batch, optimistic close, `auction-closed` event. Integration tests with a fixed clock and a concurrent bid. Covers criteria: 3, 4, 5, 7.
3. Backend: reserve on `POST /api/auctions` with validation, reserve never serialised. Covers criteria: 1, 2.
4. Backend and frontend: `CLOSED` in detail and my-bids (`WON`, `LOST`), bid rejection on closed (check in 007), result banner, bid panel hidden. Covers criteria: 6, 8, 9.

## Decisions
- Reserve is hidden from everyone. Showing "reserve met" would leak information; the banner reports only the outcome.
- Auto-close interval of 15 seconds is a kata choice; the closer is idempotent.
- Outcome names are fixed strings, not localised.

## Out of scope
- Payment, buyer fees, transfer of the car, seller notification, rescheduling an auction, manual close by the seller.
