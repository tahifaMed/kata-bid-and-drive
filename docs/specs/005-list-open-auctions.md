# 005: List open auctions

Branch: `feat/005-list-open-auctions`  
Status: implemented, pending review

## Where we are
- Merged: 001 (Postgres), 002 (design system and static page), 003 (identity and user switcher), 004 (create auction, PR #6).
- Exists: `auction` and `car` tables (V3), `Auction` domain with status `OPEN` only, `CreateAuction` with the injected `Clock`, `AuctionResponse` (the 006 detail subset, ADR 0010), `AuctionsView` and `AuctionCard` driven by the static `sampleAuctions.ts`. `AuctionListing` still has `currentBid: number`, which ADR 0004 says this spec replaces.
- Not yet: `bid` table (007), `GET /api/auctions/{id}` and the `/auctions/:id` route (006), reserve price (011), watch state (013).
- `spring-boot-starter-hateoas` is already in the backend pom.

## User story
> As a buyer, I can see all auctions that are open right now, so that I can choose which cars to bid on.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given three open auctions with different end times, when `GET /api/auctions` is called, then the response is 200 with the three items sorted by `endsAt` ascending, each with `id`, `car` (`make`, `model`, `year`, `imageUrl`), `currentBid`, `minNextBid`, `bidCount`, `endsAt` and a `self` link. With no bids yet, `currentBid` and `minNextBid` equal the starting price and `bidCount` is `0`.
2. Given five open auctions, when `GET /api/auctions?page=1&size=2` is called, then the response contains items 3 and 4 and has `next` and `prev` links. On `page=0` there is no `prev` link. On the last page (`page=2`) there is no `next` link.
3. Given `size=0`, `size=101`, `page=-1`, or a `page` or `size` that is not an integer, when the list is requested, then the response is 400 with the ProblemDetail of ADR 0011, and `errors` names `page` or `size`.
4. Given an auction whose `endsAt` is in the past (still `OPEN` in the database), when the list is requested, then that auction is not returned.
5. Given no open auctions, or a `page` after the last one, when the list is requested, then the response is 200 with an empty `_embedded.auctions` array.
6. Given `GET /api/auctions` is called without `X-User-Id`, when it runs, then the response is 200 (the list is public). A `X-User-Id` that is unknown or malformed is ignored, and the response is also 200.
7. Given the `/auctions` page loads, when the data arrives, then the page shows the cards from the API. While loading, it shows a loading state. With no open auctions it shows an empty state. When the request fails, it shows an error state with a retry action. The static `sampleAuctions.ts` is removed.
8. Given a card is clicked, when navigation happens, then the browser goes to `/auctions/:id`. The whole card is one link. The test checks the href (the route itself comes in 006).

## Domain rules and edge cases
- "Open" means `status = OPEN` and `endsAt > now` (injected `Clock`). The list does not change `status`; auto-close is 011.
- Sort by `endsAt` ascending, then `id` ascending, so pages are stable.
- `page` is 0-based and defaults to 0. `size` defaults to 20, minimum 1, maximum 100.
- `currentBid` and `minNextBid` use the rule from 004 ("highest bid, or the starting price when there are no bids"). Until 007 adds bids, the list reads no bids: `currentBid` = `minNextBid` = `startingPrice`, and `bidCount` = 0. 007 changes only where the highest bid comes from. The list does not compute these values in the UI.
- Money is a JSON string with two decimals (ADR 0004).
- The list never contains the reserve (011), the seller, the description or the mileage.
- The list shape is a HAL paged collection: `_embedded.auctions`, `page` metadata, `_links`. Spring HATEOAS omits `_embedded` when the page is empty by default, so the empty case must be tested explicitly (criterion 5).
- Time left is computed on the client from `endsAt`. The server does not return a text countdown. Display format (default, see Open questions): `2d 4h` when a day or more is left, `3h 12m` when an hour or more, `12m` otherwise, `Ending` under one minute.
- An auction that reaches its end while the page is open stays on the page until the next load. Refetching is not automatic.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| GET | /api/auctions | query `page` (default 0), `size` (default 20, 1 to 100) | 200 HAL paged collection of list items | 400 invalid paging (ADR 0011 shape) |

List item: `{id, car: {make, model, year, imageUrl}, currentBid: "12600.00", minNextBid: "12700.00", bidCount, endsAt, _links: {self}}`.

Page: `{_embedded: {auctions: [...]}, page: {size, number}, _links: {self, next?, prev?}}`. `totalElements` and `totalPages` are omitted (Q3).

Implementation shape: a `ListOpenAuctions` use case with the `Clock`, and a port method that takes the application's own paging record and returns the application's own page record. Spring Data types (`Pageable`, `Page`) stay out of the application layer (CLAUDE.md: domain and application do not depend on Spring or JPA). The controller maps the page to `PagedModel` with HAL links.

## UI (frontend)
- `AuctionsView` loads from `GET /api/auctions` through the API client (003).
- `AuctionCard` props change to a list item with string money and `endsAt`. `formatCurrency` takes a string (ADR 0004). It converts the string to a number only for display; no arithmetic.
- The types `AuctionListItem` and the page type go in `src/features/auction/types.ts`, next to `AuctionResponse`. The frontend CLAUDE.md puts feature types in feature folders, and `src/types` does not exist. Move them to shared code only when 013 needs them outside this feature.
- Delete `src/features/auction/data/sampleAuctions.ts`, the eight `public/images/auctions/*.jpg` files used only by it, the footer credit list, and the "illustrative samples" note. Update the `AuctionsView` and `AuctionCard` tests.
- Card contents in 005: title (`year make model`), time left, current bid, and the photo. The `specs`, `location` and "No reserve" badge have no data in the list API, so the card drops them (Q2). When `imageUrl` is missing, the card shows a bundled neutral placeholder at the same size (Q4).
- Paging controls: Previous and Next buttons, enabled from the `prev` and `next` links of the current page (Q1). Hidden when the page has neither link.
- Filters and tabs on the page stay non-functional until a later spec, and are out of scope here.
- Search, the watch star (013) and the "Sell a car" link are not changed by this spec, except that the link still points to `/auctions/new`.

## Slices
1. Backend: `ListOpenAuctions` use case with the `Clock`, application paging records, repository query for open auctions with `endsAt > now`, sort by `endsAt, id`, and paging. Covers criteria: 1, 4, 5.
2. Backend: `GET /api/auctions` controller, HAL `PagedModel` with `_embedded.auctions`, `next` and `prev` links, paging validation (`@Min`, `@Max`, type mismatch) mapped to the ADR 0011 shape in `AuctionExceptionHandler`, no identity required. Covers criteria: 2, 3, 6.
3. Frontend: types, fetch from the API, loading, empty and error states with retry, Previous and Next buttons, new card props, placeholder image, time left, card link, sample data and images removed, tests updated. Covers criteria: 7, 8.

## Decisions
- Open means `OPEN` and not yet ended. This keeps the list correct before the auto-close job of 011 exists.
- Public listing: reads need no identity, as the marketplace is browsable by visitors. An identity header on this GET is ignored (criterion 6).
- Invalid paging uses the ADR 0011 field-error shape. It is a field-level validation error, so no new ADR is needed.
- Before bids exist, `currentBid` and `minNextBid` are the starting price and `bidCount` is 0. 007 extends the query, and the 004 rule stays the same.
- Shared frontend types for this feature live in `features/auction/types.ts`.
- Sample photos and footer credits are removed with the sample data. Auctions without `imageUrl` show a neutral placeholder (Q4).
- The page metadata has `size` and `number` only. No count query (Q3).
- Paging controls on the page are Previous and Next (Q1).

## Answered questions
- **Q1. Paging on the page.** Previous and Next buttons, from the API's `prev` and `next` links.
- **Q2. Card fields the list does not return.** The card drops `specs`, `location` and the "No reserve" badge. The list shape does not change.
- **Q3. Page metadata.** `totalElements` and `totalPages` are omitted. Only `size` and `number` are returned.
- **Q4. Missing photo.** A neutral placeholder image at the card's size.
- **Q5. Copy (default, not yet confirmed).** Loading: "Loading auctions…". Empty: "No auctions are open right now." Error: "Could not load auctions." with a "Try again" button.

## Out of scope
- Search, filtering by make or price, sorting by the user, and the watched filter (013).
- Watch star on cards (013).
- Auto-refresh of the list or of the time left.
