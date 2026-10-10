# 010: Live auction page

Branch: `feat/010-live-auction-page`  
Status: draft

## User story
> As a buyer viewing a car, I see new bids and the countdown update live, and I am told when I have been outbid.

## Acceptance criteria
Given / When / Then. Each criterion maps to at least one test.
1. Given I am on `/auctions/5`, when another user places a bid, then the current price, bid count, minimum next bid and bid history update without a reload.
2. Given I am the highest bidder on auction 5, when a `bid-placed` event arrives with `highestBidderId` not equal to my id, then an "outbid" toast appears and the bid panel shows the new minimum.
3. Given I placed the bid in the event, when it arrives, then no outbid toast appears and the status shows "winning".
4. Given the connection drops, when `EventSource` reports an error, then the page shows "Reconnecting" and, once the connection is back, refetches the auction so bids missed while offline appear.
5. Given I leave the page, when the route changes, then the `EventSource` is closed.
6. Given an event for another auction id arrives (defensive check), when it is handled, then it is ignored.
7. Given an event with `endsAt` changed, when it arrives, then the countdown uses the new `endsAt`.
8. Given the stream fails repeatedly (5 consecutive errors without success), when that happens, then the page stops retrying, shows "Live updates unavailable" and keeps the last loaded data.

## Domain rules and edge cases
- The server is the source of truth. An event updates the displayed state only after its type and `auctionId` are checked.
- Events may be out of order or missed. Any `bid-placed` event with a `bidCount` lower than the one already shown is ignored.
- My own winning status comes from `highestBidderId` in the event, compared to the selected user id.
- Changing the selected user (003) while on the page reconnects nothing; the stream is public and the status is recalculated.

## API (backend)
Consumes `GET /api/auctions/{id}/events` (009) and `GET /api/auctions/{id}` (006).

## UI (frontend)
- Composable `src/features/auction/composables/useAuctionStream.ts`: wraps `EventSource`, typed handlers, reconnect state, cleanup on scope dispose, and the 5-error limit.
- Shared component `src/components/AppToast.vue` for the outbid message.
- `AuctionDetailView` (006) subscribes through `useAuctionStream`, applies events, and calls the refetch on reconnect.
- Live indicator: "Live" when connected, "Reconnecting" when retrying, "Live updates unavailable" when stopped.
- No Pinia store. State lives in the composable.

## Slices
1. Composable `useAuctionStream`: connect, typed events, cleanup on unmount, error counting, unit tests with a fake `EventSource`. Covers criteria: 5, 6, 8.
2. `AuctionDetailView` applies `bid-placed` to price, count, minimum and history; refetch on reconnect; `endsAt` update. Covers criteria: 1, 4, 7.
3. Outbid toast and own-bid handling using the selected user id; live indicator. Covers criteria: 2, 3.

## Decisions
- Ignoring stale events by `bidCount` is a kata choice to handle out-of-order delivery without a sequence number.
- Client trusts only the server's state. No optimistic updates from the client's own bids.

## Out of scope
- Desktop notifications, sound, live updates on the list page, push outside the auction page.
