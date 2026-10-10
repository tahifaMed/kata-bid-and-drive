# 008: My bids

Branch: `feat/008-my-bids`  
Status: draft

## User story
> As a buyer, I can see the auctions I have bid on and whether I am winning or outbid, so that I know where I stand.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given user 2 has bid on auction 5 (highest bid 1200.00, still highest) and on auction 6 (highest bid 900.00, now outbid), when `GET /api/me/bids` is called with `X-User-Id: 2`, then the response is 200 with two items sorted by `endsAt` ascending, each with `auctionId`, `car` (`make`, `model`, `year`, `imageUrl`), `myHighestBid`, `currentBid`, `status` (`WINNING` or `OUTBID`), `endsAt` and a `self` link.
2. Given user 3 has never bid, when `GET /api/me/bids` is called, then the response is 200 with an empty collection.
3. Given `GET /api/me/bids` is called without `X-User-Id`, when it runs, then the response is 401.
4. Given user 2 is viewing auction 5, when `GET /api/auctions/5` is called with `X-User-Id: 2`, then the response includes `myBid: {highestAmount, status}`. Without the header, `myBid` is absent.
5. Given user 2 is winning auction 5, when user 4 bids higher and the list is requested again, then auction 5 shows `OUTBID`.
6. Given the route `/me/bids`, when the page loads, then it lists each item with a `WINNING` or `OUTBID` badge and links to the auction. The page is linked from the app header.

## Domain rules and edge cases
- A bidder's status is `WINNING` when they are the auction's highest bidder, otherwise `OUTBID`. Bids are strictly increasing, so there are no ties.
- `myHighestBid` is the maximum of the caller's bids on that auction.
- The list includes every auction the caller has bid on, open or ended. Ended-auction results (WON or LOST) are added in 011.
- Status is read on demand. Live change is pushed by 010, not here.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| GET | /api/me/bids | none | 200 HAL collection of my-bid items | 401 |
| GET | /api/auctions/{id} | optional header | 200, now with optional `myBid` | (unchanged from 006) |

## UI (frontend)
- Route `/me/bids` (lazy `MyBidsView`) in `src/features/bidding/views/`.
- Components: `MyBidItem.vue` (status badge and link), reusing the card styling from `AuctionCard`.
- Header nav link "My bids" in the app shell, shown only when a user is selected.
- Store: none. The view calls `GET /api/me/bids` through the API client.

## Slices
1. Backend: `MyBids` query and `GET /api/me/bids` with HAL collection, sort, 401 for missing identity. Covers criteria: 1, 2, 3.
2. Backend: additive `myBid` field on `GET /api/auctions/{id}`, present only with identity. Covers criteria: 4, 5.
3. Frontend: `MyBidsView`, `MyBidItem`, route, header link. Covers criteria: 6.

## Decisions
- Status is derived from the auction's highest bidder, not stored, so it cannot drift from the bid data.
- `myBid` is additive on the 006 response, so earlier clients are unaffected.

## Out of scope
- Winning or losing after an auction closes (011), live status updates (010), bid history for the caller across filters.
