# 0015: SSE event delivery

Status: Accepted

## Context
ADR 0002 picks SSE and an in-memory broker that publishes after commit. Spec 009 leaves open how events look on the wire, which thread sends them, how the heartbeat runs and how 010 fills a bid history row. 006 shows bidders only as masked names, never as ids.

## Decision
- One SSE message per event: `event: <type>` and one `data:` line with the full envelope `{type, auctionId, occurredAt, data}`. No `id:`, because there is no replay.
- `bid-placed` carries `bidderName`, masked by the 006 rule, so a client can add a history row without a second call. `bidderId` equals `highestBidderId` on this event, so it reveals nothing new.
- The after-commit listener sends to subscribers synchronously. The broker catches any exception per emitter, completes it with the error and removes it. Nothing leaves the broker, because an exception thrown after commit would still reach the bidder's request and turn a committed bid into an error.
- One scheduled sweep sends the `keepalive` SSE comment to every open emitter, at a configurable rate.
- `PlaceBid` keeps its `@Transactional`, the one Spring import in the application layer. The after-commit listener depends on it, so every publisher of auction events runs in a transaction.
- On shutdown, every open stream is completed before the web server's graceful shutdown, which would otherwise wait its whole timeout for them. Clients reconnect.
- A stream stays open when its auction closes. 011 decides whether to complete it after sending `auction-closed`.

## Consequences
- A slow or stuck client can delay the bidder's response while its write blocks. This is acceptable for one instance with few viewers. An executor can be added behind the broker without changing the port.
- A heartbeat and an event can write to the same emitter at once. `SseEmitter` serialises its sends, so the broker adds no lock.
- A client that leaves is noticed only at the next failed write, at most one heartbeat later.
- Scheduling is enabled for the application, which 011's auto-close also needs.
- Clients parse the envelope's `type` and `auctionId`, so a new type (011) needs no change to the wire format.
