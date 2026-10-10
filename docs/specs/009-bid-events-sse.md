# 009: Bid events over SSE

Branch: `feat/009-bid-events-sse`  
Status: draft

## User story
> As a buyer on an auction page, I receive each new bid as it happens, so that I see the latest price without refreshing.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given a client subscribed to `GET /api/auctions/5/events` (`text/event-stream`), when user 2 places a bid of "1200.00" on auction 5, then the client receives one event named `bid-placed` with `auctionId: 5`, `data.bidId` equal to the bid's `id` from 007, `data.amount: "1200.00"`, `data.bidderId: 2`, `data.highestBidderId: 2`, `data.currentBid: "1200.00"`, `data.minNextBid` equal to `currentBid + minIncrement`, `data.bidCount` equal to the stored count, `data.endsAt` equal to the auction's `endsAt`, and `data.extended: false`.
2. Given a bid whose transaction rolls back (for example a version conflict from 007), when the attempt fails, then no event is sent to any subscriber, verified by a subscriber that stays connected for 1 second with no `bid-placed` received.
3. Given a client subscribed to auction 5, when a bid is placed on auction 6, then the client receives no event on auction 5 within 1 second.
4. Given an auction id that does not exist, when the stream is requested, then the response is 404 and no stream opens.
5. Given the heartbeat interval is set to 50 milliseconds in the test profile and a subscriber is idle, when the test waits 200 milliseconds, then at least one `: keepalive` comment line is received. The production default is 25 seconds.
6. Given a subscriber disconnects or its emitter completes or times out, when the broker's subscriber count is read, then it has decreased by one and no further event is sent to that emitter.
7. Given `GET /api/auctions/5/events` is called without `X-User-Id`, when it runs, then the stream opens (read-only, public).

## Domain rules and edge cases
- Event envelope, the shared contract for all auction events:
  ```json
  {"type": "bid-placed", "auctionId": 5, "occurredAt": "2026-10-10T12:00:00Z",
   "data": {"bidId": 42, "amount": "1200.00", "bidderId": 2, "highestBidderId": 2,
            "currentBid": "1200.00", "minNextBid": "1300.00", "bidCount": 4,
            "endsAt": "2026-10-10T18:00:00Z", "extended": false}}
  ```
- Field sources: `bidId` is the bid's `id` (007). `amount`, `bidderId`, `highestBidderId`, `currentBid`, `minNextBid`, `bidCount` and `endsAt` are the auction state after the bid commits. `occurredAt` is the bid's `placedAt`.
- `extended` is always `false` in this spec. Spec 012 sets it.
- The domain defines `AuctionEvent` (sealed) with a `BidPlaced` record. The application layer depends only on an `AuctionEventPublisher` port. Domain and application code never import Spring or call the broker.
- The bid use case (007) calls only the port. The adapter implements the port with Spring's `ApplicationEventPublisher`, and a `@TransactionalEventListener(phase = AFTER_COMMIT)` forwards to the in-memory broker. So nothing is sent before commit ([ADR 0002](../adr/0002-sse-over-websocket.md)).
- No replay. A client that reconnects misses events and must refetch (handled in 010).
- Subscribers are held in memory. This works for one application instance only.
- Emitter timeout 30 minutes and heartbeat 25 seconds are configurable properties. Tests override the heartbeat.
- `type` is the extension point. Spec 011 adds `auction-closed`. Spec 012 sets `extended: true` on `bid-placed`.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| GET | /api/auctions/{id}/events | none | 200 `text/event-stream` | 404 unknown auction |

## UI (frontend)
None in this spec. The stream is consumed in 010.

## Slices
1. Domain: `AuctionEvent` and `BidPlaced`; application port `AuctionEventPublisher`; in-memory broker adapter with subscribe, unsubscribe and subscriber count. Unit tests. Covers criteria: 6 (broker part).
2. Backend: adapter publishes through `ApplicationEventPublisher` with `@TransactionalEventListener(AFTER_COMMIT)`. Tests: event after commit, no event before commit, no event on rollback. Covers criteria: 1, 2, 3.
3. Backend: `GET /api/auctions/{id}/events` with `SseEmitter`, 404 check before opening, heartbeat, public access, cleanup on disconnect. Covers criteria: 4, 5, 6, 7.

## Decisions
- SSE rather than WebSocket: [ADR 0002](../adr/0002-sse-over-websocket.md).
- Events carry the full state needed by the page, so a client does not need a second call to render a bid.
- Test waits are short fixed windows (1 second for "nothing received", 200 milliseconds for the heartbeat) with the heartbeat overridden in the test profile.

## Out of scope
- Multi-instance broadcast, event replay with `Last-Event-ID`, authentication of the stream, a generic event bus.
