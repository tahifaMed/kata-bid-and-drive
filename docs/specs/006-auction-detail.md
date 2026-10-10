# 006: Auction detail

Branch: `feat/006-auction-detail`  
Status: draft

## User story
> As a buyer, I can open an auction to see the car, its current bid and the bid history, so that I can decide whether to bid.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given open auction 5 with three bids, when `GET /api/auctions/5` is called, then the response is 200 with `car`, `startingPrice`, `currentBid` (highest), `minNextBid`, `bidCount: 3`, `endsAt`, `status: "OPEN"`, `seller: {displayName}`, `highestBidderId`, `bids` (newest first, at most 50, each with `amount`, `placedAt` and masked `bidderName`), and `_links` with `self` and `place-bid` (present only while the auction is open).
2. Given an auction id that does not exist, when it is requested, then the response is 404 with a ProblemDetail.
3. Given an auction with no bids, when it is requested, then `currentBid` equals `startingPrice`, `minNextBid` equals `startingPrice`, `bidCount` is 0, `highestBidderId` is null and `bids` is empty.
4. Given a bidder named "Bob", when bids are listed, then `bidderName` is "B***".
5. Given the route `/auctions/:id` is opened, when the page loads, then it shows the car details, the price block, the bid history and a "Back to auctions" link. A not-found response shows a "Auction not found" message.
6. Given the detail page is open, when the user clicks a card on the list, then this page loads for that auction.
7. Given no `X-User-Id` header, when the detail is requested, then the response is 200 (public read).

## Domain rules and edge cases
- Bidder names are masked to the first letter followed by `***`. Bidder ids are never shown in `bids`. `highestBidderId` is shown so that the live page (010) can tell whether the viewer is still winning.
- The bid history is capped at 50 newest bids. Full history is out of scope.
- `place-bid` link is absent when the auction is closed or ended (checked with the injected `Clock`).
- Money formatting follows [ADR 0004](../adr/0004-money-as-json-string.md).

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| GET | /api/auctions/{id} | none | 200 auction detail | 404 unknown auction |

## UI (frontend)
- Route `/auctions/:id`, lazy `AuctionDetailView` in `src/features/auction/views/`.
- Components in `src/features/auction/components/`: `CarDetails.vue`, `PriceSummary.vue`, `BidHistory.vue`.
- The bid panel is added in 007 and is a placeholder slot here.
- Composable `src/features/auction/composables/useAuctionDetail.ts` fetches and holds the detail state.

## Slices
1. Backend: detail query, bid history with masking and cap, `currentBid`, `minNextBid`, `highestBidderId`. Covers criteria: 1, 3, 4.
2. Backend: 404 ProblemDetail, public access, and the `place-bid` link rule. Covers criteria: 2, 7.
3. Frontend: route, `AuctionDetailView`, components, loading and not-found states, card link. Covers criteria: 5, 6.

## Decisions
- The detail returns `highestBidderId` so that the live page can detect outbidding without another call. It is an id, not a name.
- Masking is a kata privacy choice, recorded here.

## Out of scope
- The bid form (007), "my bid" fields (008), live updates (010), closed-auction results (011).
