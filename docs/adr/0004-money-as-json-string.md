# 0004: Money as a JSON string with two decimals

Status: Accepted

## Context
Money uses `BigDecimal` in the backend. A JSON number is read as a float by JavaScript, which loses precision and rounds values such as 0.1 + 0.2.

## Decision
- Every money field in the API is a JSON string with exactly two decimals, for example `"12600.00"`.
- The backend parses and writes money as `BigDecimal` with scale 2. Input with more than two decimals is rejected with 400.
- The frontend types money as `string` and formats it for display with `Intl.NumberFormat`. It never does arithmetic on money except comparison of two server-provided values.

## Consequences
- Clients must send money as strings. Examples in specs use this form.
- The current `AuctionListing.currentBid` number in the frontend is replaced when spec 005 lands.
