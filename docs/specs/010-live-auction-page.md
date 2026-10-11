# 010: Live auction page

Branch: `feat/010-live-auction-page`  
Status: in-progress

## User story
> As a buyer viewing a car, I see new bids and the countdown update live, and I am told when I have been outbid.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given I am on `/auctions/5`, when another user places a bid, then the current bid, bid count, minimum next bid and bid history update without a reload.
2. Given I am the highest bidder on auction 5, when a `bid-placed` event arrives with `highestBidderId` not equal to my id, then an "outbid" toast appears and the bid panel shows the new minimum.
3. Given I placed the bid in the event, when it arrives, then no outbid toast appears and the bid panel shows "You are the highest bidder." (the 007 wording).
4. Given the connection drops, when `EventSource` reports an error, then the page shows "Reconnecting" and, once the connection opens again, refetches the auction so bids missed while offline appear. The first open does not refetch.
5. Given I leave the page, when the route changes, then the `EventSource` is closed.
6. Given an event for another auction id arrives (defensive check), when it is handled, then it is ignored.
7. Given an event with `endsAt` changed, when it arrives, then the time left uses the new `endsAt`. The time left ticks once per second.
8. Given the stream fails repeatedly (5 consecutive errors with no successful open between them), when that happens, then the page stops retrying, shows "Live updates unavailable" and keeps the last loaded data.

## Domain rules and edge cases
- The server is the source of truth. An event updates the displayed state only after its type and `auctionId` are checked.
- Events may be out of order or missed. A `bid-placed` event whose `bidCount` is lower than or equal to the one already shown is ignored. Equal covers the event for my own bid arriving after the refetch the bid panel does (007), so the history row is not added twice.
- An applied event sets `currentBid`, `minNextBid`, `bidCount`, `highestBidderId` and `endsAt` from `data`, and prepends `{amount: data.amount, placedAt: occurredAt, bidderName: data.bidderName}` to the bid history.
- My own winning status comes from `highestBidderId` in the event, compared to the selected user id.
- The outbid toast appears only when the shown `highestBidderId` was my id before the event and is not after it.
- Error counting: every `EventSource` error counts, and a successful open resets the count. While `readyState` is `CONNECTING` the browser retries by itself. When it is `CLOSED` (for example a non-stream response), the page opens a new `EventSource` after 3 seconds. At 5 errors the source is closed for good.
- Changing the selected user (003) while on the page reconnects nothing; the stream is public and the status is recalculated.

## API (backend)
Consumes `GET /api/auctions/{id}/events` (009) and `GET /api/auctions/{id}` (006). Event envelope (009, ADR 0015): `{type, auctionId, occurredAt, data: {bidId, amount, bidderId, bidderName, highestBidderId, currentBid, minNextBid, bidCount, endsAt, extended}}`.

## UI (frontend)
- Composable `src/features/auction/composables/useAuctionStream.ts`: wraps `EventSource`, typed handlers, reconnect state, cleanup on scope dispose, and the 5-error limit.
- Shared component `src/components/AppToast.vue` for the outbid message: `role="status"`, a dismiss button, auto-dismissed after 6 seconds.
- Shared composable `src/composables/useNow.ts`: the current time, updated once per second, so the time left counts down.
- `AuctionDetailView` (006) subscribes through `useAuctionStream`, applies events, and calls the refetch on reconnect.
- Live indicator: "Live" when connected, "Reconnecting" when retrying, "Live updates unavailable" when stopped. Nothing is shown while the first connection opens.
- No Pinia store. State lives in the composable.

## Slices
1. [x] Composable `useAuctionStream`: connect, typed events, cleanup on unmount, error counting, unit tests with a fake `EventSource`. Covers criteria: 5, 6, 8.
2. `AuctionDetailView` applies `bid-placed` to price, count, minimum and history; refetch on reconnect; `endsAt` update. Covers criteria: 1, 4, 7.
3. Outbid toast and own-bid handling using the selected user id; live indicator. Covers criteria: 2, 3.

## Decisions
- Ignoring stale events by `bidCount` is a kata choice to handle out-of-order delivery without a sequence number.
- Client trusts only the server's state. No optimistic updates from the client's own bids.
- Stale events are those with `bidCount <=` the shown one (not only lower), so an own bid already refetched is not applied twice.
- The time left ticks every second (a shared `useNow`) instead of being frozen at load time.
- The status text stays "You are the highest bidder." from 007 rather than a new "winning" label.

## Out of scope
- Desktop notifications, sound, live updates on the list page, push outside the auction page.
