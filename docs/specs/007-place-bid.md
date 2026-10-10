# 007: Place a bid

Branch: `feat/007-place-bid`  
Status: draft

## User story
> As a buyer, I can place a bid on an open auction, so that I can compete for the car.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given auction 5 with `startingPrice: "1000.00"`, `minIncrement: "100.00"` and no bids, when user 2 posts `{amount: "1000.00"}` to `/api/auctions/5/bids`, then the response is 201 with `{id, auctionId, bidderId: 2, amount: "1000.00", placedAt}`, and `GET /api/auctions/5` then shows `currentBid: "1000.00"`, `bidCount: 1` and `highestBidderId: 2`.
2. Given the current bid is "1000.00" and the increment is "100.00", when user 3 posts "1050.00", then the response is 422 with the minimum (1100.00) in the ProblemDetail, and posting "1100.00" is accepted with 201.
3. Given the caller is the seller of auction 5, when they bid on it, then the response is 403.
4. Given user 2 is the current highest bidder, when user 2 bids again, then the response is 409 with a message that they are already the highest bidder.
5. Given `endsAt` is at or before now (injected `Clock`), when a bid is posted, then the response is 409 with a message that the auction is closed.
6. Given `amount` is missing, zero, negative, or has more than two decimals, when a bid is posted, then the response is 400.
7. Given 20 threads bid at the same time on one auction with the same amount, when they finish, then exactly one bid is accepted, the others get 409 or 422, and `bidCount` and `currentBid` match the accepted bid.
8. Given no `X-User-Id` header, when a bid is posted, then the response is 401.
9. Given the bid panel, when the amount is below the minimum, then the submit button is disabled and the minimum is shown. When the server returns 409 or 422, then an error is shown and the auction is refreshed.

## Domain rules and edge cases
- Minimum bid: `startingPrice` when there are no bids, otherwise `currentBid + minIncrement`. Bids must be >= minimum.
- Bids are immutable. A bidder cannot withdraw or change a bid.
- Seller cannot bid on their own auction.
- A bidder who is already the highest bidder cannot bid again. This blocks a bidder from raising their own price.
- The bid row and the auction's updated `currentBid`, `bidCount` and `highestBidderId` are written in one transaction.
- Concurrency uses `@Version` on the auction. A version conflict returns 409 with "bid conflict, reload and retry". There is no automatic retry ([ADR 0003](../adr/0003-optimistic-locking-for-bids.md)).
- Time checks use the injected `Clock`, not the system clock.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| POST | /api/auctions/{id}/bids | `{amount: "1100.00"}` | 201 bid, `Location` header | 400 invalid amount, 401, 403 seller, 404 unknown auction, 409 closed / already highest / version conflict, 422 below minimum |

## UI (frontend)
- New feature folder `src/features/bidding/`.
- Component `src/features/bidding/components/BidPanel.vue`, placed in the detail page's slot from 006. It shows the minimum, validates the amount, and emits `placed` so the page refetches.
- The bidding feature calls the API client from 003 and holds no Pinia store.

## Slices
1. Domain: `Bid` and the minimum rule, seller rule, already-highest rule, closed rule. Unit tests with a fixed `Clock`. Covers criteria: 2, 3, 4, 5.
2. Backend: Flyway V4 adds `version` to `auction` and creates `bid`. Persistence, `PlaceBid` use case in one transaction, `POST` controller, error mapping. Covers criteria: 1, 6, 8.
3. Backend: parallel-bid integration test and the version-conflict mapping to 409. Covers criteria: 7.
4. Frontend: `BidPanel`, validation, error and refresh handling, wiring into the detail page. Covers criteria: 9.

## Decisions
- 422 for business rule violations on amount, 409 for state conflicts (closed, already highest, version conflict), 403 for role. Recorded here, consistent across the bid API.
- No automatic retry on version conflict: [ADR 0003](../adr/0003-optimistic-locking-for-bids.md).
- A bidder may not outbid themselves: a kata rule, to avoid self-raising.

## Out of scope
- Proxy (maximum) bids, buy-it-now, bid withdrawal, bid notifications (010 covers live updates), anti-sniping (012).
