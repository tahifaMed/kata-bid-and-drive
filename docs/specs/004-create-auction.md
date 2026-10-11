# 004: Create auction

Branch: `feat/004-create-auction`  
Status: implemented, pending review

## User story
> As a seller, I can put my car up for auction with a starting price, a minimum bid increment and an end time, so that buyers can bid on it.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given user 1 (Alice) is selected and the request is valid, when `POST /api/auctions` is called, then the response is 201 with a `Location: /api/auctions/{id}` header and a body with `status: "OPEN"`, `seller: {displayName: "Alice"}`, `startingPrice`, `currentBid` equal to `startingPrice`, `minNextBid` equal to `startingPrice`, `bidCount: 0`, `bids: []`, `highestBidderId: null`, and `_links` with `self` and `place-bid`. The field list is in the API section.
2. Given a request with a missing or blank make or model, a non-positive `startingPrice` or `minIncrement`, a money value with more than two decimals, a money value above `9999999999.99`, or a year outside 1886 to next year, when it is posted, then the response is 400 with a ProblemDetail whose `errors` lists every invalid field in one response.
3. Given `endsAt` is earlier than the clock's now plus one hour, or later than the clock's now plus 30 days, when the auction is posted, then the response is 400 with an error on `endsAt`. Exactly one hour and exactly 30 days are allowed.
4. Given no `X-User-Id` header, or one that is not a known user (see 003 criteria 2 to 4), when the auction is posted, then the response is 401 with the 003 ProblemDetail. Auth is checked before the body is validated, so an invalid body with no valid identity gives 401.
5. Given a valid request, when the auction is created, then one `auction` row and one `car` row are stored, and the `car` row's `auction_id` links it to that auction. A second `car` row for the same `auction_id` is rejected by the database.
6. Given the `/auctions/new` form is filled with valid data, when it is submitted, then the form shows an inline confirmation with a link to `/auctions/{id}` for the new auction.
7. Given a field is invalid and the user has left it (blur) or tried to submit, then that field shows its error and the submit button is disabled while the form is invalid.
8. Given a valid form is submitted and the server returns an error or the network fails, then the form shows "Could not create the auction. Try again." and keeps the values. Submit is disabled while the request is in flight, so one click sends one request.

## Domain rules and edge cases
- Money is `BigDecimal` with scale 2 and sent as a JSON string (see [ADR 0004](../adr/0004-money-as-json-string.md)). A JSON number (`12000.00` without quotes) is a 400. A string must match a plain decimal (`12000`, `12000.5`, `12000.00`). `1e3`, empty strings and values with more than two decimals are a 400. Trailing zeros beyond two (`12000.000`) are accepted.
- `startingPrice` > 0 and `minIncrement` > 0, at most `9999999999.99` (the column is `NUMERIC(12,2)`).
- `endsAt` must be between now + 1 hour and now + 30 days, both inclusive, using the injected `Clock`. It is an ISO-8601 instant; a non-UTC offset is converted to UTC, and a value with no offset is a 400.
- The year upper bound "next year" is computed from the injected `Clock` as well.
- The auction starts `OPEN` at creation. There is no scheduled start.
- `currentBid` is the highest bid, or `startingPrice` when there are no bids. `minNextBid` is `startingPrice` when there are no bids, otherwise `currentBid + minIncrement`. Both are defined here and used by 006 and 007.
- Car fields: `make` and `model` required, 1 to 60 characters; `year` integer in 1886 to next year; `mileageKm` integer >= 0; `description` optional, up to 2000 characters; `imageUrl` optional, an `http` or `https` URL of at most 2048 characters.
- Any user may sell. The seller is always the caller; the caller cannot choose another seller.
- Creation is not idempotent: every valid POST creates a new auction.
- There is no reserve price. 011 settled on none ([ADR 0016](../adr/0016-auction-result-on-read.md)).

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| POST | /api/auctions | `{car: {make, model, year, mileageKm, description?, imageUrl?}, startingPrice: "12000.00", minIncrement: "100.00", endsAt: "2026-10-12T18:00:00Z"}` | 201, `Location: /api/auctions/{id}`, body below | 400 validation, 401 identity |

**201 body** (a subset of the 006 detail shape, see [ADR 0010](../adr/0010-create-response-subset.md)): `id`, `status`, `car {make, model, year, mileageKm, description, imageUrl}`, `seller {displayName}`, `startingPrice`, `minIncrement`, `currentBid`, `minNextBid`, `bidCount`, `highestBidderId: null`, `endsAt`, `bids: []`, and `_links` with `self` and `place-bid`. Later specs add the fields they own; they do not remove these.

**400 body**: RFC 9457 ProblemDetail, `status: 400`, `title: "Invalid auction"`, `detail: "One or more fields are invalid"`, and `errors: [{field, message}]`. `field` is the JSON path of the request field (`car.make`, `startingPrice`, `endsAt`). Every error is returned in one response. The shape is recorded in [ADR 0011](../adr/0011-validation-error-shape.md).

## UI (frontend)
- Route `/auctions/new` (lazy `CreateAuctionView`) in `src/features/auction/`.
- Component `src/features/auction/components/AuctionForm.vue`, using client-side checks that mirror the server rules. The browser clock is used only for the endsAt hint. The server is authoritative. Its 400 is shown as the generic message in criterion 8, because the shared client does not expose the error body.
- The "Sell a car" button on `AuctionsView` becomes a `RouterLink` to `/auctions/new`.
- No Pinia store. The form keeps its own state.
- The confirmation link points at `/auctions/{id}`. That route is added by 006. Until 006 lands the link leads to the router's not-found handling; the test checks only the href.

## Slices
1. Backend domain and use case: `Auction`, `Car`, `Money`, `AuctionWindow`, `InvalidAuctionException`, `CreateAuction` with `Clock` checks for `endsAt` and the year bound. Domain unit tests with a fixed clock, including boundaries. Covers criteria: 3.
2. Backend persistence: Flyway V3 creates `auction` and `car` tables (`car.auction_id` unique, check constraints for positive money and non-negative mileage), JPA entities and the repository adapter. Testcontainers test: saving one auction stores one auction row and one car row, and a second car for the same auction is rejected. Covers criteria: 5.
3. Backend web: `POST /api/auctions` controller, request DTO, Bean Validation, ProblemDetail errors, 201 with `Location`, 401 through `@CurrentUser`. Integration tests for each error case, including the identity variants of 003. Covers criteria: 1, 2, 4.
4. Frontend: `CreateAuctionView`, `AuctionForm`, route, "Sell a car" link, confirmation, in-flight and error states. Covers criteria: 6, 7, 8.

## Decisions
- Any authenticated user may sell, so no seller role is stored. See [ADR 0009](../adr/0009-any-user-may-sell.md).
- The auction starts `OPEN` immediately. A scheduled start would be a later spec.
- `endsAt` window of 1 hour minimum and 30 days maximum, both inclusive, is a kata choice. Both bounds are configurable (`bidanddrive.auction.min-duration`, `bidanddrive.auction.max-duration`); a test sets non-default values to prove it. See [ADR 0008](../adr/0008-auction-end-window.md).
- The 201 body is a subset of the 006 shape. See [ADR 0010](../adr/0010-create-response-subset.md).
- Field-level 400 errors use the `errors` list. See [ADR 0011](../adr/0011-validation-error-shape.md).
- Money as strings: [ADR 0004](../adr/0004-money-as-json-string.md).

## Out of scope
- Editing or cancelling an auction, photo upload, reserve price, buy-it-now.
- Bid rules (own car, self-outbid, bid after close, concurrent bids): see 006 and 007.
