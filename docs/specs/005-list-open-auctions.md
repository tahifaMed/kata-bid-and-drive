# 005: List open auctions

Branch: `feat/005-list-open-auctions`  
Status: draft

## User story
> As a buyer, I can see all auctions that are open right now, so that I can choose which cars to bid on.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given three open auctions with different end times, when `GET /api/auctions` is called, then the response is 200 with the three items sorted by `endsAt` ascending, each with `id`, `car` (`make`, `model`, `year`, `imageUrl`), `currentBid`, `minNextBid`, `bidCount`, `endsAt` and a `self` link.
2. Given five open auctions, when `GET /api/auctions?page=1&size=2` is called, then the response contains items 3 and 4 and has `next` and `prev` links.
3. Given `size=101` or `page=-1`, when the list is requested, then the response is 400.
4. Given an auction whose `endsAt` is in the past (still `OPEN` in the database), when the list is requested, then that auction is not returned.
5. Given no open auctions, when the list is requested, then the response is 200 with an empty collection.
6. Given `GET /api/auctions` is called without `X-User-Id`, when it runs, then the response is 200 (the list is public).
7. Given the `/auctions` page loads, when the data arrives, then the page shows the cards from the API, with loading, empty and error states, and the static `sampleAuctions.ts` is removed.
8. Given a card is clicked, when navigation happens, then the browser goes to `/auctions/:id`.

## Domain rules and edge cases
- "Open" means `status = OPEN` and `endsAt > now` (injected `Clock`).
- Sort by `endsAt` ascending, then `id` ascending, so pages are stable.
- `size` default 20, maximum 100. `page` is 0-based.
- The list shape is a HAL paged collection: `_embedded.auctions`, `page` metadata, `_links`.
- Time left is computed on the client from `endsAt`. The server does not return a text countdown.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| GET | /api/auctions | query `page`, `size` | 200 HAL paged collection of list items | 400 invalid paging |

List item: `{id, car: {make, model, year, imageUrl}, currentBid: "12600.00", minNextBid: "12700.00", bidCount, endsAt, _links: {self}}`.

## UI (frontend)
- `AuctionsView` loads from `GET /api/auctions` through the API client from 003.
- `AuctionCard` props change to string money and `endsAt`. `formatCurrency` takes a string (see [ADR 0004](../adr/0004-money-as-json-string.md)).
- Shared `src/types` has the new `AuctionListItem` and paged response types.
- Delete `src/features/auction/data/sampleAuctions.ts` and update its tests.
- Filters and tabs on the page stay non-functional until a later spec, and are out of scope here.

## Slices
1. Backend: repository query for open auctions with paging and sort; service returns page data with the Clock. Covers criteria: 1, 4, 5, 6.
2. Backend: `GET /api/auctions` controller, HAL paged model, paging validation, `next` and `prev` links. Covers criteria: 2, 3.
3. Frontend: fetch from API, loading, empty and error states, new card types, remove sample data, card link. Covers criteria: 7, 8.

## Decisions
- Open means `OPEN` and not yet ended. This keeps the list correct before the auto-close job of 011 exists.
- Public listing: reads need no identity, as the marketplace is browsable by visitors.

## Out of scope
- Search, filtering by make or price, sorting by the user, and the watched filter (013).
