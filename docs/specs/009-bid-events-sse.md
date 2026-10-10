# 009: Bid events over SSE

Branch: `feat/009-bid-events-sse`  
Status: draft

## User story
> As a buyer on an auction page, I receive each new bid as it happens, so that I see the latest price without refreshing.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given a client subscribed to `GET /api/auctions/5/events` (`text/event-stream`), when user 2 places a bid on auction 5, then the client receives an event named `bid-placed` whose data matches the envelope below.
2. Given a bid whose transaction fails or rolls back (for example a version conflict), when the attempt fails, then no event is sent.
3. Given a client subscribed to auction 5, when a bid is placed on auction 6, then the client receives nothing.
4. Given an auction id that does not exist, when the stream is requested, then the response is 404 and no stream opens.
5. Given an idle subscriber, when 25 seconds pass, then a comment line `: keepalive` is sent.
6. Given a client disconnects or the emitter times out, when the broker is checked, then that subscriber is removed.
7. Given `GET /api/auctions/5/events` is called without `X-User-Id`, when it runs, then the stream opens (read-only, public).

## Domain rules and edge cases
- Event envelope, the shared contract for all auction events:
  ```json
  {"type": "bid-placed", "auctionId": 5, "occurredAt": "2026-10-10T12:00:00Z",
   "data": {"bidId": 42, "amount": "1200.00", "bidderId": 2, "highestBidderId": 2,
            "currentBid": "1200.00", "minNextBid": "1300.00", "bidCount": 4,
            "endsAt": "2026-10-10T18:00:00Z", "extended": false}}
  ```
- The domain defines `AuctionEvent` (sealed) with a `BidPlaced` record. The application layer depends on an `AuctionEventPublisher` port. Domain and application code never import Spring.
- Events are published **after the transaction commits**, using a transactional event listener in the adapter.
- No replay. A client that reconnects misses events and must refetch (handled in 010).
- Subscribers are held in memory. This works for one application instance only ([ADR 0002](../adr/0002-sse-over-websocket.md)).
- Emitter timeout 30 minutes, heartbeat 25 seconds, both configurable.
- `type` is the extension point. Spec 011 adds `auction-closed`. Spec 012 reuses `bid-placed` with `extended: true`.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| GET | /api/auctions/{id}/events | none | 200 `text/event-stream` | 404 unknown auction |

## UI (frontend)
None in this spec. The stream is consumed in 010.

## Slices
1. Domain: `AuctionEvent` and `BidPlaced`; application port `AuctionEventPublisher`; in-memory broker adapter with subscribe and unsubscribe. Unit tests. Covers criteria: 3, 6.
2. Backend: publish `BidPlaced` after commit from the bid use case (007) using a transactional event listener. Integration tests for success and rollback. Covers criteria: 1, 2.
3. Backend: `GET /api/auctions/{id}/events` with `SseEmitter`, 404 check before opening, heartbeat, public access. Covers criteria: 4, 5, 7.

## Decisions
- SSE rather than WebSocket: [ADR 0002](../adr/0002-sse-over-websocket.md).
- Events carry the full state needed by the page, so a client does not need a second call to render a bid.

## Out of scope
- Multi-instance broadcast, event replay with `Last-Event-ID`, authentication of the stream, a generic event bus.
