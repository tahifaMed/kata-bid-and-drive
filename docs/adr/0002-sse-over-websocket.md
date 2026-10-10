# 0002: Server-Sent Events over WebSocket for live bids

Status: Accepted

## Context
Buyers on an auction page must see new bids as they happen. Data flows only from server to browser. The backend is Spring MVC (servlet), with no WebFlux or messaging broker.

## Options considered
- **WebSocket**: bidirectional, but needs extra configuration, a STOMP or raw handler, and its own reconnect logic.
- **Server-Sent Events**: one-way over plain HTTP, supported by `SseEmitter` in Spring MVC with no new dependency. The browser `EventSource` reconnects by itself.
- **Polling**: simple, but adds latency and load.

## Decision
Use SSE. `GET /api/auctions/{id}/events` streams events. Subscribers are held in memory by an in-process broker behind an application port (`AuctionEventPublisher`). Events are published after the bid transaction commits.

## Consequences
- Works only on a single application instance. A multi-instance deployment needs a shared broker, which is out of scope.
- There is no replay. A client that reconnects may miss events, so the page refetches the auction on reconnect.
- The event envelope (`type`, `auctionId`, `occurredAt`, `data`) is the extension point for new event types.
