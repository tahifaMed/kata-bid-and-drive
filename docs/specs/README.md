# Feature roadmap

Specs are numbered in build order. Each spec is one feature, one branch (`feat/NNN-slug`) and one PR. Every spec starts as `draft` and moves to `validated` after review.

## Order and dependencies

| # | Spec | Depends on | Adds |
|---|---|---|---|
| 003 | [Identity and user switcher](003-identity-user-switcher.md) | none | `app_user`, `X-User-Id`, switcher |
| 004 | [Create auction](004-create-auction.md) | 003 | `auction`, `car`, `/auctions/new` |
| 005 | [List open auctions](005-list-open-auctions.md) | 004 | `GET /api/auctions` |
| 006 | [Auction detail](006-auction-detail.md) | 004 | `GET /api/auctions/{id}`, `/auctions/:id` |
| 007 | [Place a bid](007-place-bid.md) | 006 | `bid`, `auction.version`, bid panel |
| 008 | [My bids](008-my-bids.md) | 007 | `GET /api/me/bids`, `/me/bids` |
| 009 | [Bid events over SSE](009-bid-events-sse.md) | 007 | event envelope, `GET .../events` |
| 010 | [Live auction page](010-live-auction-page.md) | 009, 008 | `useAuctionStream`, outbid toast |
| 011 | [Reserve and auto-close](011-reserve-and-auto-close.md) | 009 | reserve, closer, `auction-closed` |
| 012 | [Anti-sniping](012-anti-sniping.md) | 009, 010 | extension rule, `extended` flag |
| 013 | [Watchlist](013-watchlist.md) | 005 | `watch`, `/api/me/watchlist` |

Dependency graph:

```
003 → 004 → 005 ─────────────────────────→ 013
          ├→ 006 → 007 → 008 ─┐
                       └→ 009 ┴→ 010 → 012
                              └→ 011
```

Specs 011, 012 and 013 are additive. Each owns its own migration (V5, none, V6) and touches shared files only in the ways its spec lists.

## Shared conventions
- **Money**: `BigDecimal`, scale 2, sent as a JSON string ([ADR 0004](../adr/0004-money-as-json-string.md)).
- **Time**: `Instant`, ISO-8601 UTC. The server uses an injected `Clock`.
- **Errors**: RFC 9457 ProblemDetail. 400 invalid input, 401 missing or unknown identity, 403 wrong role, 404 unknown resource, 409 state conflict, 422 business rule on amount.
- **Identity**: `X-User-Id` header ([ADR 0001](../adr/0001-identity-user-switcher.md)). Read endpoints for auctions are public. Anything under `/api/me` needs identity.
- **Hypermedia**: HAL responses with `_links`. Collections use HAL paged models.
- **Paging**: `page` is 0-based, `size` defaults to 20 and is capped at 100.
- **Live events**: SSE with envelope `{type, auctionId, occurredAt, data}` ([ADR 0002](../adr/0002-sse-over-websocket.md)). Event types: `bid-placed` (009), `auction-closed` (011).

## Decision index
- [ADR 0001](../adr/0001-identity-user-switcher.md): identity via seeded users and header.
- [ADR 0002](../adr/0002-sse-over-websocket.md): SSE over WebSocket for live bids.
- [ADR 0003](../adr/0003-optimistic-locking-for-bids.md): optimistic locking for bids.
- [ADR 0004](../adr/0004-money-as-json-string.md): money as JSON strings.
- [ADR 0005](../adr/0005-bid-error-mapping.md): bid status codes and check order.
- [ADR 0006](../adr/0006-bidding-rules.md): seller cannot bid, no self-outbid, first-bid rule.
