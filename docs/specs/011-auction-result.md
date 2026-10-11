# 011: Auction result

Branch: `feat/011-auction-result`  
Status: done

## User story
> As a buyer or a seller, I can see how an auction ended once its end time has passed, so that everyone knows who won and at what price.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given auction 6 has `endsAt` at or before now (injected `Clock`) and a highest bid of "5200.00" by user 3 ("Bob"), when `GET /api/auctions/6` is called, then the response is 200 with `status: "CLOSED"`, `outcome: "SOLD"`, `winnerName: "B***"`, `highestBidderId: 3`, `currentBid: "5200.00"` and no `place-bid` link.
2. Given auction 7 has ended with no bids, when it is requested, then `status` is `CLOSED`, `outcome` is `NO_BIDS`, the `winnerName` key is omitted, `highestBidderId` is null, `currentBid` equals `startingPrice` and there is no `place-bid` link.
3. Given an open auction, when it is requested, then `status` is `OPEN` and the `outcome` and `winnerName` keys are omitted (006 shape unchanged). The 201 response of `POST /api/auctions` (004) always has `status: "OPEN"` and omits both keys.
4. Given `endsAt` equals now, when the detail is requested, then `status` is `CLOSED`. Given `endsAt` is 1 ms after now, then `status` is `OPEN` and `place-bid` is present. A bid is accepted only when the detail would report `OPEN` at the same instant (007 returns 409 closed otherwise).
5. Given user 3 is the highest bidder on ended auction 6 and user 2 also bid on it, when `GET /api/me/bids` is called, then user 3 sees `WON` and user 2 sees `LOST` for auction 6. `myBid.status` on `GET /api/auctions/6` gives the same values. Given an auction whose `endsAt` is 1 ms after now, the statuses stay `WINNING` and `OUTBID` (008), in both endpoints.
6. Given a closed auction's page, when it loads, then a result banner shows "Sold to B*** for 5200.00" or "No bids", the bid panel is not rendered and "Minimum next bid" is not shown. For `SOLD` the current bid is labelled "Final price". For `NO_BIDS` it keeps the label "Starting price". Every viewer sees the same banner, including the seller and the winner.
7. Given the auction page is open and the countdown reaches `endsAt`, when time runs out, then the page fetches the detail and shows the result banner when it returns `CLOSED`. If it returns `OPEN` with a later `endsAt` (extended, 012), the page keeps the countdown. If it returns `OPEN` with the same `endsAt` (client clock ahead of the server), the page fetches again after 2 seconds, at most 3 times for that `endsAt`. A failed fetch counts as one attempt. Once the local time reaches `endsAt`, the bid panel is hidden even if no fetch has succeeded. After the last attempt the page keeps its last data, and any bid still gets 409 (007).
8. Given a `bid-placed` event arrives while the page shows `CLOSED`, when it is received, then the page refetches the detail instead of patching fields, so the banner, `winnerName` and `currentBid` agree. No outbid toast is shown while `CLOSED`.
9. Given the `/me/bids` page, when an item is `WON` or `LOST`, then it shows a "Won" badge (winning style) or a "Lost" badge (outbid style) instead of "Winning" or "Outbid". The "Ended" chip stays.

## Domain rules and edge cases
- The stored `AuctionStatus` stays `{OPEN}`, and the database still allows only `'OPEN'`. `CLOSED` is never persisted.
- A new derived enum `AuctionPhase {OPEN, CLOSED}` comes from `Auction.phaseAt(now)`: `CLOSED` when `endsAt <= now`, otherwise `OPEN`. `isBiddableAt(now)` delegates to it (`phaseAt(now) == OPEN`), so the bid check (007) and every read use one rule.
- Nothing is written when an auction ends. Phase, outcome and winner are worked out on read from `endsAt`, the stored bid totals and the injected `Clock` ([ADR 0016](../adr/0016-auction-result-on-read.md)).
- Outcome is set only when `CLOSED`: `NO_BIDS` when `bidCount` is 0, otherwise `SOLD`. The winner is `highestBidderId`, and the final price is `currentBid`.
- `winnerName` is the winner's display name masked with `BidderName.mask` (006). `highestBidderId` is unchanged.
- `BidderStanding.status(Instant now)` gives `WON` or `LOST` when the auction's phase is `CLOSED`, otherwise `WINNING` or `OUTBID`. `BidderStatus` gains `WON` and `LOST`.
- Each use case reads `clock.instant()` once per request and uses that instant for everything it returns. `GetAuctionDetail` uses it for `status`, `outcome`, `place-bid` and `myBid.status`. `ListMyBids` gets the `Clock` injected and uses it for every item. Web DTOs receive computed values and never call `status(...)` themselves.
- Carriers to the web layer: `AuctionDetail(auction, sellerName, bids, AuctionPhase phase, AuctionOutcome outcome, String winnerName, BidderStanding myBid, BidderStatus myBidStatus)`, where `outcome`, `winnerName`, `myBid` and `myBidStatus` are nullable. It replaces the `biddable` flag. `GetAuctionDetail` builds `winnerName` through `UserDirectory`. `ListMyBids` returns `List<MyBidView(BidderStanding standing, BidderStatus status)>`. The create controller (004) builds `AuctionDetail` with `OPEN`.
- Accepted window: a bid that passes the check just before `endsAt` can commit just after it. A read in between can show the previous leader as `SOLD` or `WON`, and the result changes when the bid commits. This lasts about one transaction. Criterion 8 corrects the live page, and other pages correct themselves on the next read.
- Anti-sniping (012) only moves `endsAt`, so it needs no change here: an extended auction is simply still open.
- Ended auctions are not in the open list. 005 already filters by `endsAt`.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| GET | /api/auctions/{id} | none | `status` can be `CLOSED`, adds optional `outcome` and `winnerName` (keys omitted when absent) | (unchanged) |
| POST | /api/auctions | (unchanged) | 201 with `status: "OPEN"`, no `outcome` or `winnerName` | (unchanged) |
| GET | /api/me/bids | `X-User-Id` | `status` can also be `WON` or `LOST` | (unchanged) |

No new endpoint, no migration and no new event type.

## UI (frontend)
- The frontend `AuctionResponse` type gains `status: 'OPEN' | 'CLOSED'`, `outcome?: 'SOLD' | 'NO_BIDS'` and `winnerName?: string`. The my-bids status type and `AuctionResponse.myBid.status` both gain `'WON' | 'LOST'`.
- `AuctionResultBanner.vue` in `src/features/auction/components/`, shown by `AuctionDetailView` when `status` is `CLOSED`. The bid panel is hidden, as 006 already does for an auction that is not biddable.
- `PriceSummary` hides "Minimum next bid" when `CLOSED`, and labels the current bid "Final price" for `SOLD` or "Starting price" for `NO_BIDS`.
- The bid panel shows only when `place-bid` is present and the local time is before `endsAt`.
- `AuctionDetailView` refetches at zero as criterion 7 describes, and refetches on `bid-placed` while `CLOSED` (criterion 8).
- `MyBidItem` maps `WON` to the winning style and `LOST` to the outbid style.

## Slices
1. [x] Domain: `AuctionPhase`, `Auction.phaseAt(now)` with `isBiddableAt` delegating, `AuctionOutcome` (`SOLD`, `NO_BIDS`), `BidderStanding.status(now)` with `WON` and `LOST`. Unit tests with a fixed clock. Covers criteria: 1, 2, 4, 5 (rules).
2. [x] Backend: `AuctionDetail` and `MyBidView` carry the computed values. `GetAuctionDetail` and `ListMyBids` use one instant per request. `AuctionResponse.status` becomes `AuctionPhase`, and `place-bid` keys off `phase == OPEN`. The detail returns `status`, `outcome` and `winnerName`; the create response is `OPEN`; my-bids and `myBid` show `WON` and `LOST`. Web and integration tests with a fixed clock. Covers criteria: 1, 2, 3, 4, 5.
3. [x] Frontend: types, `AuctionResultBanner`, `PriceSummary` closed state, bid panel hidden at local `endsAt`, refetch at zero with retry, refetch without a toast on `bid-placed` while closed, `MyBidItem` badges. Covers criteria: 6, 7, 8, 9.
4. [x] Docs and comments: replace the stale 011 notes (scheduled closer, `auction-closed`, reserve, V5). The notes are in 004, 005, 006, 007 (the closed rule), 008, 009 and ADR 0015. The comments are in `Auction.java`, `BidderStatus.java`, `AuctionDetail.java`, `AuctionController.java`, `MyBidItem.vue` and `bidding/types.ts`. No criteria.

## Decisions
- The result is worked out on read, not stored by a scheduled job ([ADR 0016](../adr/0016-auction-result-on-read.md)). There is no closer and one time rule is shared with 007. The window of about one transaction is accepted (see Domain rules).
- `CLOSED` is a derived phase, not a stored status. The database constraint and `AuctionStatus` stay unchanged.
- There is no reserve price. The kata does not ask for one, and without it the outcome has only two values.
- There is no `auction-closed` SSE event. The page knows `endsAt` and refetches when it passes. Streams stay open as before. ADR 0016 supersedes the 011 parts of ADR 0015.
- The banner is the same for every viewer, with no "You won" variant. My bids already shows `WON` to the winner.
- `/me/bids` does not refetch when an auction ends. An item can show "Winning" next to "Ended" until the page is reloaded. That is accepted for this slim scope.
- `outcome` and `winnerName` are additive on the 006 response, as ADR 0010 allows.

## Out of scope
- Reserve price, scheduled closing, the `auction-closed` event, a stored outcome or closing date.
- Payment, buyer fees, transfer of the car, seller notification, rescheduling, manual close by the seller.
