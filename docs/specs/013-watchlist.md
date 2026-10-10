# 013: Watchlist

Branch: `feat/013-watchlist`  
Status: draft

## User story
> As a buyer, I can watch auctions and see my watched list, so that I can follow the cars I am interested in.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given user 2 and open auction 5, when `PUT /api/me/watchlist/5` is called, then the response is 204. Calling it again is also 204 and changes nothing.
2. Given user 2 watches auction 5, when `DELETE /api/me/watchlist/5` is called, then the response is 204 and auction 5 is removed. Deleting an auction that is not watched is also 204.
3. Given user 2 watches auctions 5 and 6, when `GET /api/me/watchlist` is called, then the response is 200 with both auctions as list items (same shape as 005), sorted by `endsAt`, including closed ones.
4. Given an auction id that does not exist, when `PUT /api/me/watchlist/{id}` is called, then the response is 404.
5. Given no `X-User-Id` header, when any `/api/me/watchlist` call is made, then the response is 401.
6. Given user 2 is viewing auction 5, when `GET /api/auctions/5` is called with `X-User-Id: 2`, then the response includes `watched: true` or `watched: false`. Without the header, `watched` is absent.
7. Given user 2 calls `GET /api/auctions?watched=true`, when it runs, then only open auctions they watch are returned, paged as in 005. Without the header it is 401.
8. Given the star on an auction card or the detail page is clicked, when the request succeeds, then the star state changes at once. If the request fails, then the state is reverted and an error is shown.

## Domain rules and edge cases
- A watch is a pair (user, auction). The pair is unique; watching is idempotent.
- Watching is allowed for open and closed auctions. The watched list includes both; the `watched=true` filter on the open list is the only place open-only applies.
- Removing a user's watch does not affect bids.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| PUT | /api/me/watchlist/{auctionId} | none | 204 | 401, 404 unknown auction |
| DELETE | /api/me/watchlist/{auctionId} | none | 204 | 401 |
| GET | /api/me/watchlist | none | 200 HAL collection of list items | 401 |
| GET | /api/auctions | adds optional `watched=true` (needs identity) | 200 (unchanged shape) | 401 when `watched=true` without identity |
| GET | /api/auctions/{id} | optional header | adds optional `watched` | (unchanged) |

## UI (frontend)
- `WatchStar.vue` in `src/components/`, shared by `AuctionCard` (005) and `AuctionDetailView` (006). It is hidden when no user is selected.
- Optimistic toggle with revert on error.
- Watch list page: none in this spec. The watched list is exposed through the `watched=true` filter on `/auctions`, with a toggle control on that page.

## Slices
1. Backend: Flyway V6 creates `watch` (composite primary key `user_id`, `auction_id`). Idempotent `PUT` and `DELETE` with 404 check. Integration tests. Covers criteria: 1, 2, 4, 5.
2. Backend: `GET /api/me/watchlist`, `watched` on detail, `watched=true` filter on the open list, 401 rules. Covers criteria: 3, 6, 7.
3. Frontend: `WatchStar`, optimistic toggle with revert, watched filter toggle on `/auctions`. Covers criteria: 8.

## Decisions
- The composite key makes watching idempotent without a separate check.
- Watched filter on the existing list rather than a new page, to keep the surface small.

## Out of scope
- Notifications when a watched auction ends or gets a bid, watch counts shown publicly, sorting the watched list by the user.
