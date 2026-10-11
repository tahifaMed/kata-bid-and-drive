# 011: Auction result

Branch: `feat/011-auction-result`  
Status: draft

## User story
> As a buyer or a seller, I can see how an auction ended once its end time has passed, so that everyone knows who won and at what price.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given auction 6 has `endsAt` at or before now (injected `Clock`) and a highest bid of "5200.00" by user 3 ("Bob"), when `GET /api/auctions/6` is called, then the response is 200 with `status: "CLOSED"`, `outcome: "SOLD"`, `winnerName: "B***"`, `currentBid: "5200.00"` and no `place-bid` link.
2. Given auction 7 has ended with no bids, when it is requested, then `status` is `CLOSED`, `outcome` is `NO_BIDS`, the `winnerName` key is omitted and `currentBid` equals `startingPrice`.
3. Given an open auction, when it is requested, then `status` is `OPEN` and the `outcome` and `winnerName` keys are omitted (006 shape unchanged).
4. Given `endsAt` equals now, when the detail is requested, then `status` is `CLOSED`. Given `endsAt` is 1 ms after now, then `status` is `OPEN`. A bid is accepted only when the detail would report `OPEN` at the same instant (007 returns 409 closed otherwise).
5. Given user 3 is the highest bidder on ended auction 6 and user 2 also bid on it, when `GET /api/me/bids` is called, then user 3 sees `WON` and user 2 sees `LOST` for auction 6. `myBid.status` on `GET /api/auctions/6` gives the same values. On open auctions the statuses stay `WINNING` and `OUTBID` (008).
6. Given a closed auction's page, when it loads, then a result banner shows "Sold to B*** for 5200.00" or "No bids", and the bid panel is not rendered.
7. Given the auction page is open and the countdown reaches `endsAt`, when time runs out, then the page fetches the detail once and shows the result banner. If the fetch still returns `OPEN` (the auction was extended, 012), the page keeps the countdown and shows no banner.
8. Given the `/me/bids` page, when an item is `WON` or `LOST`, then it shows a "Won" or "Lost" badge instead of "Winning" or "Outbid".

## Domain rules and edge cases
- An auction is `CLOSED` when its stored `status` is not `OPEN` or `endsAt <= now`, otherwise `OPEN`. This is the existing biddable rule from 006 and 007 (`Auction.isBiddableAt`), now exposed as `Auction.statusAt(now)`. The bid check and every read use the same rule, so no response reports `OPEN` for an auction that would reject a bid.
- Nothing is written when an auction ends. Status, outcome and winner are worked out on read from `endsAt`, the stored bid totals and the injected `Clock` ([ADR 0016](../adr/0016-auction-result-on-read.md)).
- Outcome, set only when closed: `NO_BIDS` when `bidCount` is 0, otherwise `SOLD`. The winner is `highestBidderId`, and the final price is `currentBid`.
- `winnerName` is the winner's display name masked with `BidderName.mask` (006). The winner id is still available as `highestBidderId`.
- `BidderStanding` (008) takes the closed flag: closed and highest gives `WON`, closed and not highest gives `LOST`, and open keeps `WINNING` or `OUTBID`.
- Anti-sniping (012) only moves `endsAt`, so it needs no change here: an extended auction is simply still open.
- Ended auctions are not in the open list. 005 already filters by `endsAt`.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| GET | /api/auctions/{id} | none | `status` can be `CLOSED`, adds optional `outcome` and `winnerName` (keys omitted when absent) | (unchanged) |
| GET | /api/me/bids | `X-User-Id` | `status` can also be `WON` or `LOST` | (unchanged) |

No new endpoint, no migration and no new event type.

## UI (frontend)
- The frontend `AuctionResponse` type gains `status: 'OPEN' | 'CLOSED'`, `outcome?: 'SOLD' | 'NO_BIDS'` and `winnerName?: string`. The my-bids status type gains `'WON' | 'LOST'`.
- `AuctionResultBanner.vue` in `src/features/auction/components/`, shown by `AuctionDetailView` when `status` is `CLOSED`. The bid panel is hidden, as 006 already does for an auction that is not biddable.
- `AuctionDetailView` refetches the detail once when the countdown reaches zero (criterion 7).
- `MyBidItem` maps `WON` and `LOST` to badges.

## Slices
1. Domain: `Auction.statusAt(now)`, `AuctionOutcome` (`SOLD`, `NO_BIDS`), `WON` and `LOST` in `BidderStanding`. Unit tests with a fixed clock. Covers criteria: 1, 2, 4, 5 (rules).
2. Backend: `status`, `outcome` and `winnerName` on the detail, `WON` and `LOST` in my-bids and `myBid`. Web and integration tests with a fixed clock. Covers criteria: 1, 2, 3, 4, 5.
3. Frontend: types, `AuctionResultBanner`, refetch at zero, `MyBidItem` badges. Covers criteria: 6, 7, 8.

## Decisions
- The result is worked out on read, not stored by a scheduled job. No closer, no race with late bids, and one time rule shared with 007 ([ADR 0016](../adr/0016-auction-result-on-read.md)).
- There is no reserve price. The kata does not ask for one, and without it the outcome has only two values.
- There is no `auction-closed` SSE event. The page knows `endsAt` and refetches when it passes. This supersedes the 011 notes in 009 and ADR 0015, and streams stay open as before.
- `outcome` and `winnerName` are additive on the 006 response, as ADR 0010 allows.

## Out of scope
- Reserve price, scheduled closing, the `auction-closed` event, a stored outcome or closing date.
- Payment, buyer fees, transfer of the car, seller notification, rescheduling, manual close by the seller.
