# 007: Place a bid

Branch: `feat/007-place-bid`  
Status: in-progress

## User story
> As a buyer, I can place a bid on an open auction, so that I can compete for the car.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given auction 5 (seller user 1) with `startingPrice: "1000.00"`, `minIncrement: "100.00"` and no bids, when user 2 posts `{"amount": "1000.00"}` to `/api/auctions/5/bids`, then:
   - the response is 201 with `{id, auctionId, bidderId: 2, amount: "1000.00", placedAt, _links: {self, auction}}` and a `Location` header equal to `_links.self`;
   - `GET /api/auctions/5` then shows `sellerId: 1`, `currentBid: "1000.00"`, `minNextBid: "1100.00"`, `bidCount: 1` and `highestBidderId: 2`;
   - `GET /api/auctions` shows `bidCount: 1` for auction 5.
2. Given the current bid is "1000.00" and the increment is "100.00", when user 3 posts "1050.00", then the response is 422 with a ProblemDetail property `minimumBid: "1100.00"`. Posting "1100.00" is accepted with 201.
3. Given user 1 is the seller of auction 5, when user 1 bids on it, then the response is 403.
4. Given user 2 is the current highest bidder, when user 2 bids again, then the response is 409 with a ProblemDetail stating they are already the highest bidder.
5. Given `endsAt` is at or before now (injected `Clock`), when user 3 bids, then the response is 409 stating the auction is closed. This holds even when the amount is also below the minimum: the closed check runs first.
6. Given `amount` is any of the following, when a bid is posted, then the response is 400 with title "Invalid bid" and `errors[{field: "amount", message}]` ([ADR 0011](../adr/0011-validation-error-shape.md)):
   - missing, `""`, `"abc"`, `" 1100"` or `"1e3"`
   - zero or negative
   - more than two significant decimals (`"1100.001"`)
   - above `"9999999999.99"`
   - a JSON number instead of a string

   `"1100.000"` is accepted as `"1100.00"`, the same rule as create auction. A body that is not JSON at all is 400 with the same title and `errors: []`.
7. Given 20 distinct bidders post the same amount at the same time on one open auction, when all requests finish, then:
   - exactly one response is 201;
   - every other response is 422 or 409;
   - `bidCount` is 1 and `currentBid` equals the winning amount;
   - the `bid` table holds exactly one row for that auction.

   Only 4 users are seeded, so the test inserts its own 20 `app_user` rows.
8. Identity and lookup:
   - No `X-User-Id` header, or an unknown id, on the `POST` or the single-bid `GET`: 401.
   - An auction id that does not exist: 404, even when the body is also invalid (for example `{"amount": 1100}` or `{}`).
   - `GET /api/auctions/{id}/bids/{bidId}` returns the bid to its bidder. It is 404 for any other user and for a bid not in that auction ([ADR 0013](../adr/0013-bid-resource-visibility.md)).
9. Given user 2 double-clicks submit so the same bid is posted twice, when both requests finish, then one is 201 and the other is 409, and `bidCount` is 1.
   - Sent one after the other, the second 409 says already highest.
   - Sent in parallel, it may instead be the version conflict, so the parallel test asserts the status only.
10. Bid panel (component tests with Vitest):
    - When the amount is empty, malformed or below the minimum, the submit button is disabled and the minimum (`minNextBid`) is shown.
    - On 201, the input is cleared, a confirmation is shown and `placed` is emitted.
    - On 409 or 422, the ProblemDetail's `detail` is shown and `placed` is emitted so the page refetches.
    - On 400, the `errors[].message` is shown.
    - On 401, 403 or a network failure, a generic message is shown and `placed` is not emitted.
    - When the viewer is the seller, the panel shows "You are the seller" instead of the form.
    - When the viewer is the highest bidder, the panel shows "You are the highest bidder" instead of the form.
    - Switching user in the user switcher re-evaluates the panel without a page reload.
11. Detail page (Vitest): when the panel emits `placed`, the page refetches the auction through `useAuctionDetail.refresh()`. The page stays in the `ready` state, without showing "Loading auction…", and shows the new totals.

## Domain rules and edge cases
- Minimum bid: `startingPrice` when there are no bids, otherwise `currentBid + minIncrement`. The first bid may equal `startingPrice`. Later bids must be >= the minimum.
- Bids are immutable. A bidder cannot withdraw or change a bid.
- "Closed" means the auction is not biddable: `Auction.isBiddableAt(now)` is false (`status` is not `OPEN`, or `endsAt <= now`). `placeBid` reuses that method, so 011 needs no change here.
- The seller cannot bid on their own auction. This is an ownership rule, not a role ([ADR 0006](../adr/0006-bidding-rules.md)).
- A bidder who is already the highest bidder cannot bid again ([ADR 0006](../adr/0006-bidding-rules.md)).
- Check order when several rules apply: identity (401), auction exists (404), body valid (400), seller (403), closed (409), already highest (409), minimum (422), version conflict (409). See [ADR 0005](../adr/0005-bid-error-mapping.md).
- **Keeping 404 ahead of 400.** The request record is `PlaceBidRequest(JsonNode amount)`, bound without `@Valid`. A `String` field would trip the global no-coercion rule and fail while parsing. The controller turns the node into a plain value, `RawAmount(String text)`, where `text` is null when the amount is missing or not a JSON string. The application layer never sees Jackson types. The `PlaceBid` use case looks up the auction first, then validates the text with the create-auction rule and throws `InvalidBidException(field, message)`. The one exception is a body that is not a JSON object at all (not JSON, `[]`, `"x"`): it fails while parsing and gives 400 before the lookup.
- Amounts above `9999999999.99` are 400, the column limit. An auction whose minimum would exceed that can take no more bids: every valid amount gets 422 with that `minimumBid`.
- **Versioning.** The domain `Auction` carries `version` as a plain value. The JPA entity in the persistence adapter owns `@Version`. Domain code never imports JPA ([ADR 0003](../adr/0003-optimistic-locking-for-bids.md)).
- **One write.** `AuctionRepository.recordBid(Auction, Bid)` writes the bid row and the auction's new `currentBid`, `bidCount` and `highestBidderId` in one transaction. It loads the entity. If the stored version differs from `auction.version()`, it throws a version conflict right away, because Hibernate checks against the version it loaded, not the field value. Otherwise it copies the totals, and `@Version` guards the remaining race at flush.
- **Version conflict.** It can surface at flush or at commit, outside the use case, as Spring's `ObjectOptimisticLockingFailureException`. The bid controller's exception handler maps it to 409 with "bid conflict, reload and retry". There is no automatic retry.
- Time checks use the injected `Clock`, not the system clock.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| POST | /api/auctions/{id}/bids | `{"amount": "1100.00"}` | 201 bid, `Location` header | 400 invalid body, 401 identity, 403 seller, 404 unknown auction, 409 closed / already highest / version conflict, 422 below minimum (`minimumBid`) |
| GET | /api/auctions/{id}/bids/{bidId} | `X-User-Id` | 200 bid | 401 identity, 404 unknown auction, bid not in that auction, or not the caller's bid |

- The bid response carries `id`. The `bid-placed` event (009) names the same value `bidId`.
- `_links.self` and `Location` point at the `GET` above. `_links.auction` points at the auction.
- Errors come from a `BidExceptionHandler` scoped to the bid controller. Its 400 title is "Invalid bid". It also maps `AuctionNotFoundException` to 404.
- The detail response from 006 gains `sellerId`, so the panel can tell that the viewer is the seller.
- `GET /api/auctions` (005) shows the real `bidCount` from the auction row.

## UI (frontend)
- **Feature folder:** new `src/features/bidding/`.
- **`BidPanel.vue`** (`src/features/bidding/components/`):
  - The detail page renders it in the section 006 left for it, only when `place-bid` is present. The router mounts the page directly, so nothing could fill the 006 slot. The slot is removed and the page renders the panel itself, and the 006 view tests move to the panel.
  - It shows `minNextBid` as the minimum, validates the amount, posts to the `place-bid` link, and emits `placed`.
  - It compares `sellerId` and `highestBidderId` with the identity store's `currentUserId` reactively.
- **`useAuctionDetail.refresh()`:** refetches with the existing stale-response guard and keeps `state` at `ready`, so the panel and its message stay on screen. A failed refresh keeps the last auction shown.
- **API client:** the bidding feature calls the 003 API client and holds no Pinia store. `ApiError` gains an optional parsed ProblemDetail (`title`, `detail`, extra properties). When the error body is not JSON (a proxy 502, for example), `problem` is undefined and the status still drives handling.

## Slices
1. Done. **Domain** (criteria 2, 3, 4, 5).
   - `Auction` gains the stored components `leadingAmount` (`Money`, null with no bids), `leadingBidderId` (`UserId`, null with no bids), `bidCount` and `version`.
   - They are named apart from the 006 accessors, which keep their contract: `currentBid()` returns `startingPrice` with no bids (006 AC3), and `highestBidderId()` returns `Optional<UserId>`.
   - Update the call sites: `Auction.open`, `JpaAuctionRepository.toDomain` and the 006 tests.
   - `Bid` gains `auctionId`, and `id` becomes a `Long` that is null until saved.
   - `Auction.placeBid(bidder, amount, now)` returns the updated auction and the new bid, or throws one of the bid rejections, in the check order above.
   - Unit tests use a fixed `Clock`.
2. **Persistence** (criterion 1, storage part).
   - Flyway V4 adds to `auction`: `current_bid NUMERIC(12,2) NULL`, `bid_count INT NOT NULL DEFAULT 0 CHECK (bid_count >= 0)`, `highest_bidder_id BIGINT NULL REFERENCES app_user` and `version BIGINT NOT NULL DEFAULT 0`.
   - V4 creates `bid(id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, auction_id BIGINT NOT NULL REFERENCES auction, bidder_id BIGINT NOT NULL REFERENCES app_user, amount NUMERIC(12,2) NOT NULL CHECK (amount > 0), placed_at TIMESTAMPTZ NOT NULL)`, with an index on `(auction_id, placed_at DESC, id DESC)`.
   - `AuctionEntity` owns `@Version`. `AuctionRepository.recordBid` is added. A JPA bid adapter replaces `NoStoredBids`, with `findNewest` and `findById(auctionId, bidId)`.
   - The Flyway version check in `PostgresConfigurationTests` moves to 4.
   - Testcontainers tests cover a round trip, and a stale version failing with an optimistic-lock exception.
3. **Backend API, place bid** (criteria 1 except the list, 2–6, 8 for `POST`, 9 sequential).
   - `PlaceBid` use case in one transaction.
   - `POST` controller with `RawAmount`, HAL `_links` and `Location`.
   - `BidExceptionHandler`: 400 "Invalid bid", 401, 403, 404, 409, and 422 with `minimumBid`.
4. **Backend API, read side** (criteria 1 `sellerId` and list `bidCount`, 8 for `GET`).
   - `GET` single bid under [ADR 0013](../adr/0013-bid-resource-visibility.md).
   - `sellerId` on the detail, real `bidCount` on the list.
5. **Backend concurrency** (criteria 7, 9 parallel).
   - Version-conflict mapping to 409.
   - 20-bidder parallel test and the parallel double post, against a real server and database, not transactional.
6. **Frontend** (criteria 10, 11).
   - `ApiError` with the ProblemDetail, `BidPanel`, `useAuctionDetail.refresh()`, and wiring into the detail page in place of the 006 slot.
   - Vitest component and view tests.

## Decisions
- Status code mapping and check order: [ADR 0005](../adr/0005-bid-error-mapping.md).
- Ownership, no self-outbid, and first-bid rule: [ADR 0006](../adr/0006-bidding-rules.md).
- No automatic retry on version conflict: [ADR 0003](../adr/0003-optimistic-locking-for-bids.md).
- A single bid is visible only to its bidder: [ADR 0013](../adr/0013-bid-resource-visibility.md).
- `amount` is validated after the auction lookup so that 404 wins over 400 (see Domain rules).
- Bid amounts use the same format rule as create auction, so `"1100.000"` is accepted.
- `sellerId` is added to the detail response. The seller and the highest bidder are both visible to the UI, which needs them to choose what the panel shows.

## Out of scope
- Proxy (maximum) bids, buy-it-now, bid withdrawal, bid notifications (010 covers live updates), anti-sniping (012).
