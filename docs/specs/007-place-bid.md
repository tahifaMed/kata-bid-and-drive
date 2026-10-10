# 007: Place a bid

Branch: `feat/007-place-bid`  
Status: draft

## User story
> As a buyer, I can place a bid on an open auction, so that I can compete for the car.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given auction 5 with `startingPrice: "1000.00"`, `minIncrement: "100.00"` and no bids, when user 2 posts `{"amount": "1000.00"}` to `/api/auctions/5/bids`, then the response is 201 with `{id, auctionId, bidderId: 2, amount: "1000.00", placedAt, _links: {self, auction}}`, and `GET /api/auctions/5` then shows `currentBid: "1000.00"`, `bidCount: 1` and `highestBidderId: 2`.
2. Given the current bid is "1000.00" and the increment is "100.00", when user 3 posts "1050.00", then the response is 422 with a ProblemDetail property `minimumBid: "1100.00"`. Posting "1100.00" is accepted with 201.
3. Given user 1 is the seller of auction 5, when user 1 bids on it, then the response is 403.
4. Given user 2 is the current highest bidder, when user 2 bids again, then the response is 409 with a ProblemDetail stating they are already the highest bidder.
5. Given `endsAt` is at or before now (injected `Clock`), when user 3 bids, then the response is 409 stating the auction is closed. This holds even when the amount is also below the minimum: the closed check runs first.
6. Given `amount` is missing, zero, negative, has more than two decimals, or is a JSON number instead of a string, when a bid is posted, then the response is 400.
7. Given 20 distinct bidders post the same amount at the same time on one open auction, when all requests finish, then exactly one response is 201, every other response is 422 or 409, and `bidCount` is 1 with `currentBid` equal to the winning amount.
8. Given no `X-User-Id` header, or an unknown id, when a bid is posted, then the response is 401. Given an auction id that does not exist, the response is 404.
9. Given user 2 double-clicks submit so the same bid is posted twice, when both requests finish, then one is 201 and the other is 409 (already highest), and `bidCount` is 1.
10. Given the bid panel (component test with Vitest), when the amount is below the minimum, then the submit button is disabled and the minimum is shown. When the API returns 409 or 422, then an error message is shown and the `placed` event is emitted so the page refetches the auction.

## Domain rules and edge cases
- Minimum bid: `startingPrice` when there are no bids, otherwise `currentBid + minIncrement`. The first bid may equal `startingPrice`. Later bids must be >= the minimum.
- Bids are immutable. A bidder cannot withdraw or change a bid.
- "Closed" means `endsAt <= now` in this spec. Spec 011 extends it with `status = CLOSED`.
- The seller cannot bid on their own auction. This is an ownership rule, not a role ([ADR 0006](../adr/0006-bidding-rules.md)).
- A bidder who is already the highest bidder cannot bid again ([ADR 0006](../adr/0006-bidding-rules.md)).
- Check order when several rules apply: identity (401), auction exists (404), body valid (400), seller (403), closed (409), already highest (409), minimum (422), version conflict (409). See [ADR 0005](../adr/0005-bid-error-mapping.md).
- The auction's version is a plain `version` value in the domain `Auction`. The JPA entity in the persistence adapter owns `@Version`. Domain code never imports JPA ([ADR 0003](../adr/0003-optimistic-locking-for-bids.md)).
- The bid row and the auction's updated `currentBid`, `bidCount`, `highestBidderId` and `version` are written in one transaction.
- A version conflict returns 409 with "bid conflict, reload and retry". There is no automatic retry.
- Time checks use the injected `Clock`, not the system clock.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| POST | /api/auctions/{id}/bids | `{"amount": "1100.00"}` | 201 bid, `Location` header | 400 invalid body, 401 identity, 403 seller, 404 unknown auction, 409 closed / already highest / version conflict, 422 below minimum (`minimumBid`) |

The bid response carries `id`. The `bid-placed` event (009) names the same value `bidId`.

## UI (frontend)
- New feature folder `src/features/bidding/`.
- Component `src/features/bidding/components/BidPanel.vue`, placed in the detail page's slot from 006. It shows the minimum, validates the amount, and emits `placed` so the page refetches.
- The bidding feature calls the API client from 003 and holds no Pinia store.

## Slices
1. Domain: `Auction` with a `version` value, `Bid`, the minimum rule, seller, closed and already-highest rules with the check order above. Unit tests with a fixed `Clock`. Covers criteria: 2, 3, 4, 5.
2. Persistence: Flyway V4 adds `version` to `auction` and creates `bid`. JPA entities own `@Version`; repository maps to and from the domain. Integration test with Testcontainers. Covers criteria: 1 (storage part).
3. Backend API: `PlaceBid` use case in one transaction, `POST` controller, HAL `_links`, 400 validation, 401, 404, ProblemDetail mapping including `minimumBid`. Covers criteria: 1, 6, 8, 9.
4. Backend concurrency: 20-bidder parallel test and version-conflict mapping to 409. Covers criteria: 7.
5. Frontend: `BidPanel`, validation, error and refresh handling, wiring into the detail page. Vitest component test. Covers criteria: 10.

## Decisions
- Status code mapping and check order: [ADR 0005](../adr/0005-bid-error-mapping.md).
- Ownership, no self-outbid, and first-bid rule: [ADR 0006](../adr/0006-bidding-rules.md).
- No automatic retry on version conflict: [ADR 0003](../adr/0003-optimistic-locking-for-bids.md).

## Out of scope
- Proxy (maximum) bids, buy-it-now, bid withdrawal, bid notifications (010 covers live updates), anti-sniping (012).
