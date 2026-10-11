# 008: My bids

Branch: `feat/008-my-bids`  
Status: implemented, pending review

## User story
> As a buyer, I can see the auctions I have bid on and whether I am winning or outbid, so that I know where I stand.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test. Money is a two-decimal string (ADR 0004).
1. Given user 2 has bid on auction 5 (user 2's highest bid "1200.00", still the highest, `currentBid: "1200.00"`) and on auction 6 (user 2's highest bid "900.00", user 4 then bid "1000.00", `currentBid: "1000.00"`), and auction 6 ends before auction 5, when `GET /api/me/bids` is called with `X-User-Id: 2`, then the response is 200 with `_embedded.myBids` holding two items in the order 6, 5. Each item has `auctionId`, `car` (`make`, `model`, `year`, `imageUrl`), `myHighestBid`, `currentBid`, `status` (`WINNING` for 5, `OUTBID` for 6), `endsAt` and `_links.auction` to `/api/auctions/{id}`. The collection has `_links.self` to `/api/me/bids`.
2. Given user 2 bid "1000.00" on auction 5, was outbid by user 4 with "1100.00", then bid "1200.00", when the list is requested, then auction 5 appears once with `myHighestBid: "1200.00"` and `status: WINNING`.
3. Given two auctions user 2 bid on have the same `endsAt`, when the list is requested, then they are ordered by `auctionId` ascending.
4. Given user 3 has never bid, when `GET /api/me/bids` is called, then the response is 200 with an empty `_embedded.myBids`.
5. Given `GET /api/me/bids` is called without `X-User-Id`, or with an unknown or malformed id, when it runs, then the response is 401.
6. Given user 2 has bid on auction 5, when `GET /api/auctions/5` is called with `X-User-Id: 2`, then the response includes `myBid: {highestAmount, status}`. In these cases the `myBid` key is omitted (not `null`) and the response is still 200: no header, an unknown or malformed id, or a user with no bid on that auction (the seller included). The 201 create response (004) also omits `myBid`.
7. Given user 2 is winning auction 5, when user 4 bids higher and the list and the detail are requested again, then auction 5 shows `OUTBID` in both.
8. Given the route `/me/bids`, when the page loads:
   - while loading, it shows "Loading your bids…";
   - each item shows "{year} {make} {model}", a "Winning" or "Outbid" badge, "Your bid {myHighestBid}", "Current bid {currentBid}", the time left or "Ended" when `endsAt` is not in the future, and links to `/auctions/{auctionId}`;
   - an empty list shows "You have not placed any bids yet.";
   - a failed request, a 401 included, shows "Could not load your bids." with a "Try again" button that repeats the request.
9. Given the page is open, when the selected user changes, then the list reloads for the new user. A late response for the previous user is not shown.
10. Given any page with the app header, when it renders, then the header has a "My bids" link to `/me/bids`, next to the "Auctions" link and the user switcher.

## Domain rules and edge cases
- A bidder's status is `WINNING` when they are the auction's highest bidder, otherwise `OUTBID`. Bids are strictly increasing, so there are no ties. The rule lives in a domain type (`BidderStanding`), used by both the list and the detail.
- `myHighestBid` is the maximum of the caller's bids on that auction.
- The list includes every auction the caller has bid on, open or ended. Since 011, ended auctions show `WON` or `LOST`. The UI shows "Ended" instead of the time left.
- Sort is `endsAt` ascending, then `auctionId` ascending.
- The auction and the caller's highest bid are read in one SQL statement, so a concurrent bid cannot pair `WINNING` with a `myHighestBid` below `currentBid`. Two statements in one transaction would not be enough under Postgres READ COMMITTED.
- A malformed `X-User-Id` never queries the user list (003).
- Status is read on demand. Live change is pushed by 010, not here.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| GET | /api/me/bids | `X-User-Id` | 200 `{_embedded: {myBids: [...]}, _links: {self}}`, not paged | 401 |
| GET | /api/auctions/{id} | optional `X-User-Id` | 200, now with optional `myBid` (key omitted when absent) | (unchanged from 006) |

## UI (frontend)
- A shared `AppHeader` in `src/components/` replaces the per-view headers. It holds the logo, "Auctions", "My bids" and the user switcher, plus a slot for view actions ("Sell a car" and search on the list page). A user is always selected (003), so the link is always shown.
- Route `/me/bids` (lazy `MyBidsView`) in `src/features/bidding/views/`.
- Components: `MyBidItem.vue` (status badge and link), reusing the card styling from `AuctionCard`.
- Store: none. The view calls `GET /api/me/bids` through the API client and reloads when the selected user changes.
- The frontend `AuctionResponse` type gains `myBid?: {highestAmount, status}`. The detail page does not use it yet.

## Slices
1. Done. Backend: `BidderStanding` domain type, `BidRepository` port for the caller's highest bid per auction, `ListMyBids`, `GET /api/me/bids` with HAL collection, sort, and 401 for missing, unknown or malformed identity. Covers criteria: 1, 2, 3, 4, 5, 7 (list).
2. Done. Backend: optional identity (`@CurrentUser(required = false)`), `GetAuctionDetail.execute(id, viewer)`, additive `myBid` on `GET /api/auctions/{id}` with `@JsonInclude(NON_NULL)`. Covers criteria: 6, 7 (detail).
3. Done. Frontend: `AppHeader` extracted and used by every view. Covers criteria: 10.
4. Done. Frontend: `MyBidsView`, `MyBidItem`, route, `myBid?` type. Covers criteria: 8, 9.

## Decisions
- Status is derived from the auction's highest bidder, not stored, so it cannot drift from the bid data.
- `myBid` is additive on the 006 response, so earlier clients are unaffected (ADR 0010 allows additions).
- The detail stays public and ignores an invalid identity instead of answering 401: [ADR 0014](../adr/0014-optional-identity-on-public-reads.md).
- `/api/me/bids` is not paged: one buyer's bids are few. This is an exception to the paged-collection convention.
- Each item links to its auction with `_links.auction`, as 007 does for a bid. The item is not a resource of its own.
- No new migration. The bidder query scans `bid` by `bidder_id`, which is fine at kata scale. V6 stays with 013; 011 needs no migration.

## Out of scope
- Winning or losing after an auction closes (011), live status updates (010), using `myBid` on the detail page, bid history for the caller across filters, paging.
