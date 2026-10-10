# 004: Create auction

Branch: `feat/004-create-auction`  
Status: draft

## User story
> As a seller, I can put my car up for auction with a starting price, a minimum bid increment and an end time, so that buyers can bid on it.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given user 1 is selected and the request is valid, when `POST /api/auctions` is called, then the response is 201 with a `Location` header, `status: "OPEN"`, `seller` set to user 1, `currentBid` equal to `startingPrice`, `bidCount: 0`, and a `self` link.
2. Given a request with a missing make, a non-positive `startingPrice`, a non-positive `minIncrement`, a money value with more than two decimals, or a year outside 1886 to next year, when it is posted, then the response is 400 with a ProblemDetail listing each invalid field.
3. Given `endsAt` is less than one hour after the clock's now, or more than 30 days after it, when the auction is posted, then the response is 400.
4. Given no `X-User-Id` header, when the auction is posted, then the response is 401.
5. Given a valid request, when the auction is created, then the car is stored with it and no other auction exists for that car (a car belongs to exactly one auction).
6. Given the `/auctions/new` form is filled with valid data, when it is submitted, then the user sees a confirmation with a link to the new auction.
7. Given the form fields are invalid, when the user tries to submit, then the submit button is disabled and each field shows its error.

## Domain rules and edge cases
- Money is `BigDecimal` with scale 2 and sent as a string (see [ADR 0004](../adr/0004-money-as-json-string.md)).
- `startingPrice` > 0; `minIncrement` > 0.
- `endsAt` must be between now + 1 hour and now + 30 days, using the injected `Clock`.
- The auction starts `OPEN` at creation. There is no scheduled start.
- `currentBid` is the highest bid, or `startingPrice` when there are no bids. `minNextBid` is `startingPrice` when there are no bids, otherwise `currentBid + minIncrement`. Both are defined here and used by 006 and 007.
- Car fields: `make` and `model` required, 1 to 60 characters; `year` integer in 1886 to next year; `mileageKm` integer >= 0; `description` optional, up to 2000 characters; `imageUrl` optional, must be an `http` or `https` URL.
- Any user may sell. The seller is always the caller; the caller cannot choose another seller.
- The reserve price is not part of this spec (see 011).

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| POST | /api/auctions | `{car: {make, model, year, mileageKm, description?, imageUrl?}, startingPrice: "12000.00", minIncrement: "100.00", endsAt: "2026-10-12T18:00:00Z"}` | 201 auction (see 006 shape), `Location` header | 400 validation, 401 |

## UI (frontend)
- Route `/auctions/new` (lazy `CreateAuctionView`) in `src/features/auction/`.
- Component `src/features/auction/components/AuctionForm.vue`, using client-side checks that mirror the server rules.
- The "Sell a car" button on `AuctionsView` becomes a `RouterLink` to `/auctions/new`.
- No Pinia store. The form keeps its own state.

## Slices
1. Backend: Flyway V3 creates `auction` and `car` tables (car has a unique `auction_id`), with check constraints for positive money. Domain `Auction` and `CreateAuction` use case with `Clock` checks for `endsAt`. Domain unit tests. Covers criteria: 3, 5.
2. Backend: `POST /api/auctions` controller, request DTO, validation, ProblemDetail errors, 201 with `Location`. Integration test for each error case. Covers criteria: 1, 2, 4.
3. Frontend: `CreateAuctionView`, `AuctionForm`, route, "Sell a car" link, confirmation. Covers criteria: 6, 7.

## Decisions
- Any authenticated user may sell, so no seller role is stored.
- The auction starts `OPEN` immediately. A scheduled start would be a later spec.
- `endsAt` window of 1 hour minimum and 30 days maximum is a kata choice, recorded here. Both bounds are configurable properties.
- Money as strings: [ADR 0004](../adr/0004-money-as-json-string.md).

## Out of scope
- Editing or cancelling an auction, photo upload, reserve price (011), buy-it-now.
