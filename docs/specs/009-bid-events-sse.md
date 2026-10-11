# 009: Bid events over SSE

Branch: `feat/009-bid-events-sse`  
Status: validated

## User story
> As a buyer on an auction page, I receive each new bid as it happens, so that I see the latest price without refreshing.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test. "Received" is checked with Awaitility and an upper bound of 2 seconds. "Not received" is checked with a marker: after the step under test, a valid bid is placed on the same auction, and the first `bid-placed` the client sees must be the marker's. Stream lines are parsed by the SSE rules, so `event:bid-placed` and `event: bid-placed` are the same (one optional space after the colon; Spring writes none).
1. Given a client subscribed to `GET /api/auctions/5/events` (`text/event-stream`), when user 2 places a bid of "1200.00" on auction 5, then the client receives one SSE message with the line `event: bid-placed` and one `data:` line whose JSON has `type: "bid-placed"`, `auctionId: 5`, `occurredAt` equal to the bid's `placedAt`, `data.bidId` equal to the bid's `id` from 007, `data.amount: "1200.00"`, `data.bidderId: 2`, `data.bidderName` equal to user 2's display name masked as in 006 (for example "B***"), `data.highestBidderId: 2`, `data.currentBid: "1200.00"`, `data.minNextBid` equal to `currentBid + minIncrement`, `data.bidCount` equal to the stored count, `data.endsAt` equal to the auction's `endsAt`, and `data.extended: false`.
2. Given a bid whose transaction rolls back after `PlaceBid` has published, when the transaction ends, then no event is sent to any subscriber. The test calls the real `PlaceBid` bean inside an outer `TransactionTemplate` that marks the transaction rollback-only after `execute` returns. A second case lets the commit fail on a version conflict: another bid on the same auction commits between `execute` and the outer commit. In both cases the marker check holds.
3. Given a bid that 007 rejects (malformed amount: 400, seller bids: 403, unknown auction: 404, closed or already highest: 409, too low: 422), when it is refused, then no event is sent. The marker check holds.
4. Given a client subscribed to auction 5, when a bid is placed on auction 6, then the client receives nothing on auction 5. The marker check holds, with the marker bid placed on auction 5.
5. Given a subscriber whose emitter fails on send, and a second, healthy subscriber on the same auction, when a bid is placed, then the bid succeeds (`PlaceBid.execute` returns the bid; over HTTP the response is 201), the healthy subscriber receives the event, and the broken subscriber is removed.
6. Given an auction id that does not exist, when the stream is requested with `Accept: text/event-stream`, then the response is 404 with content type `application/problem+json` and title "Auction not found", and no subscriber is added. Given a non-numeric id, the response is 400.
7. Given the heartbeat interval is set to 50 milliseconds through the test's properties and a subscriber is idle, then a `keepalive` comment line (written by Spring as `:keepalive`) is received within 2 seconds. The production default is 25 seconds.
8. Given a subscribed emitter, when it completes, times out or reports an error, then the broker's subscriber count for that auction drops by one and no further event is sent to it.
9. Given a client that closes its connection, when the next heartbeat or event is sent, then the server notices the failed write and the subscriber count drops by one.
10. Given `GET /api/auctions/5/events` is called without `X-User-Id`, or with a malformed or unknown one, when it runs, then the stream opens with 200 (public read, [ADR 0014](../adr/0014-optional-identity-on-public-reads.md)).

## Domain rules and edge cases
- Event envelope, the shared contract for all auction events:
  ```json
  {"type": "bid-placed", "auctionId": 5, "occurredAt": "2026-10-10T12:00:00Z",
   "data": {"bidId": 42, "amount": "1200.00", "bidderId": 2, "bidderName": "B***", "highestBidderId": 2,
            "currentBid": "1200.00", "minNextBid": "1300.00", "bidCount": 4,
            "endsAt": "2026-10-10T18:00:00Z", "extended": false}}
  ```
- Wire format: each event is one SSE message with `event: <type>` and a single `data:` line holding the full envelope as JSON. There is no SSE `id:` field, because there is no replay. The heartbeat is an SSE comment with the text `keepalive`, sent with `SseEmitter.event().comment("keepalive")`.
- Field sources: `bidId` is the id of the bid returned by `recordBid`. `bidderName` is the bidder's display name masked with `BidderName.mask` (006), so 010 can add a history row without a second call. `bidderId` always equals `highestBidderId` on this event, so it reveals nothing that the detail does not. `amount`, `highestBidderId`, `currentBid`, `minNextBid`, `bidCount` and `endsAt` are the auction state after the bid. `occurredAt` is the bid's `placedAt`.
- `extended` is always `false` in this spec. Spec 012 sets it.
- The domain defines `AuctionEvent` (sealed) with a `BidPlaced` record. It holds domain types (`Money`, `UserId`, `Instant`). The `type` string (`bid-placed`), the JSON names and the money-as-string rule ([ADR 0004](../adr/0004-money-as-json-string.md)) live in an adapter DTO, not in the domain.
- The application layer depends only on an `AuctionEventPublisher` port and never calls the broker. `PlaceBid` gets two new constructor arguments, `UserDirectory` and `AuctionEventPublisher`, wired in `AuctionConfiguration.placeBid`. After `recordBid`, it looks the bidder up, masks the name and publishes `BidPlaced`. The web layer has already rejected an unknown bidder with 401 (ADR 0001), so a missing user here is broken data: it throws `IllegalStateException`, the transaction rolls back (500), and no event is sent.
- `PlaceBid.execute` keeps its existing `@Transactional`, the one Spring import allowed in the application layer. Publishing relies on it: the listener runs only after that transaction commits. A test goes through the real `PlaceBid` bean to prove the event fires.
- The adapter implements the port with Spring's `ApplicationEventPublisher`. A `@TransactionalEventListener(phase = AFTER_COMMIT)` forwards to the in-memory broker, so nothing is sent before commit ([ADR 0002](../adr/0002-sse-over-websocket.md)). Without an active transaction the event is dropped (`fallbackExecution` stays false). Every publisher of auction events must run in a transaction; this includes the scheduled closer in 011.
- Delivery is synchronous in the after-commit listener. The broker catches any `Exception` per emitter, calls `completeWithError` and removes it. No exception leaves the broker, so a committed bid always gets its 201 and other subscribers still receive the event.
- The broker lives in `auction/adapter/out/events`, because it uses Spring web types. It depends on a small `EventSink` interface (send an event, send a comment, complete with error, and register completion, timeout and error callbacks). Production wraps an `SseEmitter`; unit tests use a fake sink that fires the callbacks, since `SseEmitter` fires them only once a servlet container is attached.
- Subscribers are held in a `ConcurrentHashMap<Long, CopyOnWriteArrayList<EventSink>>`, so a subscribe or unsubscribe during a send is safe. An auction's entry is removed when its list becomes empty. The subscriber count is per auction.
- A heartbeat and an event can write to the same emitter at once. `SseEmitter` serialises sends internally, so the broker adds no lock.
- The server learns that a client left only when a write fails. That happens at the next heartbeat or event.
- Any existing auction can be subscribed to, whatever its status. Only an unknown id gives 404. The check runs before the emitter is created, through a new application use case `CheckAuctionExists` that throws `AuctionNotFoundException` (the controller does not call the repository). It needs an `AuctionRepository.existsById` port method.
- The stream's 404 must not be lost to content negotiation (`produces = text/event-stream`). The existing `ProblemDetail` handler is enough: Spring writes a `ProblemDetail` as `application/problem+json` even when the request accepts only `text/event-stream`, and a test pins this. On subscribe the broker sends the comment `connected`, because Spring flushes an SSE response only on its first write. This opens the stream at once, so a client knows it is subscribed. A sink that fails on connect is not kept.
- A stream stays open when its auction closes. 011 sends `auction-closed` on it and decides whether to complete the emitter after that.
- The heartbeat is one scheduled sweep that sends the `keepalive` comment to every open emitter. Scheduling is enabled in an adapter configuration class.
- No replay. A client that reconnects misses events and must refetch (handled in 010).
- Subscribers are held in memory. This works for one application instance only.
- Emitter timeout 30 minutes and heartbeat 25 seconds are configurable properties (`bidanddrive.events.emitter-timeout`, `bidanddrive.events.heartbeat`). Tests override the heartbeat with `@TestPropertySource`. A test class with its own properties gets its own Spring context, whose sweep only touches that context's broker. The class is marked `@DirtiesContext` so the 50 millisecond scheduler stops when it finishes.
- `type` is the extension point. Spec 011 adds `auction-closed`. Spec 012 sets `extended: true` on `bid-placed`.

## API (backend)
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| GET | /api/auctions/{id}/events | none | 200 `text/event-stream` | 400 non-numeric id, 404 unknown auction (`application/problem+json`) |

The endpoint is served by a new `AuctionEventsController`. It is added to the `assignableTypes` of `AuctionExceptionHandler`, which already maps `AuctionNotFoundException` (404) and `MethodArgumentTypeMismatchException` (400). The 404 is sent as `application/problem+json`.

## UI (frontend)
None in this spec. The stream is consumed in 010.

## Slices
1. [x] Domain and broker: `AuctionEvent` and `BidPlaced`; the port `AuctionEventPublisher`; the `EventSink` seam and the in-memory broker with subscribe, unsubscribe, subscriber count, broadcast, heartbeat and failure isolation. Unit tests with fake sinks. Covers criteria: 5 (broker part), 8.
2. [x] Publishing after commit: `PlaceBid` publishes through the port; the adapter uses `ApplicationEventPublisher` and an `AFTER_COMMIT` listener that forwards to the broker. Integration tests through the real `PlaceBid` bean, with a fake sink subscribed on the broker. Covers criteria: 1 (event content), 2, 3, 4, 5 (`execute` returns, healthy sink receives).
3. [x] Endpoint: `CheckAuctionExists` and `existsById`; `AuctionEventsController` with an `SseEmitter` wrapped as an `EventSink`; the envelope DTO; 404 and 400 before opening; public access. Tests on a real server (`RANDOM_PORT`) with an HTTP client reading the stream. Covers criteria: 1 (end to end), 5 (HTTP 201), 6, 10.
4. Heartbeat: `EventsProperties`, `@EnableScheduling` in an adapter configuration class, and the sweep. Real-server tests with a 50 millisecond heartbeat. Covers criteria: 7, 9.
5. Deploy: a regex `location` for `/api/auctions/\d+/events` in `frontend/nginx.conf.template`, placed before `/api/`, with `proxy_buffering off`, `proxy_cache off`, `gzip off`, `proxy_http_version 1.1`, `proxy_set_header Connection ""` and a `proxy_read_timeout` above the heartbeat ([ADR 0012](../adr/0012-railway-deployment.md)). No criterion. Checked with `nginx -t` and a manual stream on Railway, which also confirms that Railway's edge does not cut the stream early. A client reconnects in any case (010).

## Decisions
- SSE rather than WebSocket: [ADR 0002](../adr/0002-sse-over-websocket.md).
- Wire format, synchronous delivery with failure isolation, the heartbeat sweep and the masked `bidderName`: [ADR 0015](../adr/0015-sse-event-delivery.md).
- Events carry the full state needed by the page, so a client does not need a second call to render a bid.
- Waits avoid fixed sleeps. "Received" uses Awaitility with a 2 second bound. "Not received" uses a marker bid placed afterwards.

## Out of scope
- Multi-instance broadcast, event replay with `Last-Event-ID`, authentication of the stream, a generic event bus.
