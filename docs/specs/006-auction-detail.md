# 006: Auction detail

Branch: `feat/006-auction-detail`  
Status: validated

## Where we are
- Merged: 004 (create auction, `AuctionResponse` is the detail shape per ADR 0010) and 005 (list open auctions, card links to `/auctions/:id`).
- Exists: `auction` and `car` tables (V3), `app_user` with `display_name` (V2), `Auction.currentBid()` and `minNextBid()` that return the starting price. `AuctionRepository` has `save` and `findOpen`, no `findById`.
- Not yet: the `bid` table and the stored bid totals. Spec 007 creates both in V4 and keeps `currentBid`, `bidCount` and `highestBidderId` on the auction row (ADR 0003). Until then the detail reads no bids from storage, the same approach as 005.

## User story
> As a buyer, I can open an auction to see the car, its current bid and the bid history, so that I can decide whether to bid.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given open auction 5, when `GET /api/auctions/5` is called, then the response is 200 with `id`, `car` (`make`, `model`, `year`, `mileageKm`, `description`, `imageUrl`), `startingPrice`, `minIncrement`, `currentBid`, `minNextBid`, `bidCount`, `endsAt`, `status: "OPEN"`, `seller: {displayName}`, `highestBidderId`, `bids` (each with `amount`, `placedAt` and masked `bidderName`) and `_links` with `self` and `place-bid`. The web test uses a detail with three bids to check the mapping. The bids are faked there, because storage has none until 007.
2. Given an auction id that does not exist, including `0` and negative ids, when it is requested, then the response is 404 with a ProblemDetail. Given an id that is not a whole number (`/api/auctions/abc`) or is too large for a 64-bit integer, the response is 400 with the ADR 0011 shape and `errors[0].field` is `id`.
3. Given an auction with no bids, when it is requested, then `currentBid` equals `startingPrice`, `minNextBid` equals `startingPrice`, `bidCount` is 0, `highestBidderId` is null and `bids` is empty.
4. Given a bidder named "Bob", when bids are listed, then `bidderName` is "B***".
5. Given the route `/auctions/:id` is opened, when the page loads, then it shows the car details, the price block (current bid, minimum next bid, bid count, time left), the bid history and a "Back to auctions" link. While loading it shows "Loading auction…". A 404 shows "This auction was not found." with the "Back to auctions" link. Any other failure shows "Could not load this auction." with a "Try again" button.
6. Given the list page, when the user clicks a card, then the router resolves `/auctions/:id` to the detail page and the page loads that auction.
7. Given no `X-User-Id` header, when the detail is requested, then the response is 200 (public read). An unknown or malformed `X-User-Id` is ignored.
8. Given an auction whose `endsAt` is at or before now (injected `Clock`), when it is requested, then the response is 200 with its stored `status` and no `place-bid` link. The page shows "Ended" as time left, and the bid panel slot is not rendered.
9. Given 51 bids, when the history is built, then it has the 50 newest bids, newest first.
10. Given two bids with the same `placedAt`, when the history is built, then the bid with the higher id comes first.

## Domain rules and edge cases
- Bidder names are masked to the first character (Unicode code point) followed by `***`. An empty name masks to `***`. Bidder ids are never shown in `bids`. `highestBidderId` is shown so that the live page (010) can tell whether the viewer is still winning.
- The seller's `displayName` is shown in full. The seller id is not returned.
- The seller and the bidders are foreign keys to `app_user`, so they always exist. If the `UserDirectory` lookup still finds none, the use case throws `IllegalStateException`, which gives a 500 without a stack trace in the body.
- The bid history is the 50 newest bids, sorted by `placedAt` descending, then by bid id descending. Full history is out of scope.
- `currentBid`, `bidCount` and `highestBidderId` come from the `Auction`, not from the bid history. In 006 they are the starting price, 0 and null, because no bids are stored. 007 stores them on the auction row and changes these methods, together with `minNextBid` (`currentBid + minIncrement` once a bid exists, [ADR 0006](../adr/0006-bidding-rules.md)).
- "Biddable" means `status = OPEN` and `endsAt > now` (injected `Clock`), the same rule as the 005 list. The `place-bid` link is present only when biddable. The stored `status` is returned unchanged; auto-close is 011.
- Money is a JSON string with two decimals ([ADR 0004](../adr/0004-money-as-json-string.md)).
- Time left on the page uses the 005 format (`2d 4h`, `3h 12m`, `12m`, `Ending`), and `Ended` once `endsAt` has passed.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| GET | /api/auctions/{id} | none | 200 auction detail | 400 non-numeric id (ADR 0011 shape), 404 unknown auction |

Body: the `AuctionResponse` shape from ADR 0010. `place-bid` is `{"href": ".../api/auctions/5/bids"}`; the method (POST) is documented with the API, not in the link.

Implementation shape:
- Domain: `Bid` (id, bidder id, amount, placed at), `BidHistory.newest(bids)` (sort and cap at 50), `BidderName.mask`, and `Auction.bidCount()`, `highestBidderId()`, `isBiddableAt(Instant)`. 007 reuses this `Bid`.
- Application: `GetAuctionDetail` use case with the `Clock`, `AuctionRepository.findById`, a `BidRepository` port (`findByAuctionId`) and the identity `UserDirectory`. It returns an `AuctionDetail` read model (auction, seller name, masked history, biddable flag) and throws `AuctionNotFoundException` for an unknown id.
- Persistence: `AuctionRepository.findById` with the car loaded, tested with Testcontainers. The `BidRepository` adapter returns no bids until 007 replaces it.
- Web: `AuctionResponse` is built from `AuctionDetail`. The 004 create response goes through the same mapping, with a new auction's detail (seller name, no bids, biddable).

## UI (frontend)
- Route `/auctions/:id`, lazy `AuctionDetailView` in `src/features/auction/views/`.
- Components in `src/features/auction/components/`: `CarDetails.vue`, `PriceSummary.vue`, `BidHistory.vue`. A missing photo uses the 005 placeholder.
- The bid panel is added in 007. Here it is a placeholder slot, rendered only when the `place-bid` link is present.
- Composable `src/features/auction/composables/useAuctionDetail.ts` fetches and holds the detail state.

## Slices
1. Done. Backend domain and application: `Bid`, `BidHistory`, `BidderName`, `Auction` bid getters and biddable rule, `GetAuctionDetail`, `AuctionNotFoundException`, and `AuctionRepository.findById` with a Testcontainers test (the port change needs the adapter). Covers criteria: 3, 4, 8, 9, 10 (domain part).
2. Done. Backend web: the empty `BidRepository` adapter, `GET /api/auctions/{id}`, 404 and 400 ProblemDetail, public access, conditional `place-bid` link, `AuctionResponse` built from `AuctionDetail`. Covers criteria: 1, 2, 3, 7, 8.
3. Frontend: route, `AuctionDetailView`, components, loading, not-found and error states, time left, bid panel slot. Covers criteria: 5, 6, 8.

## Decisions
- The detail returns `highestBidderId` so that the live page can detect outbidding without another call. It is an id, not a name.
- Masking is a kata privacy choice, recorded here.
- An ended auction that is still `OPEN` in storage is returned with its stored status and without `place-bid`. No `ENDED` value is added before 011. ADR 0010 now says "a biddable auction always has `place-bid`".
- The bid totals belong to the `Auction` (007, ADR 0003). The bid history only feeds the `bids` list. One source of truth for the totals.
- Bids are read through a port that returns none until 007, like the 005 list.
- A path variable error in the ADR 0011 shape uses the parameter name as `field` (`id`), as 005 does for query parameters.

## Out of scope
- The bid form (007), "my bid" fields (008), live updates (010), closed-auction results (011).
