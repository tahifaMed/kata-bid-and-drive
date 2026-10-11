# 0016: Auction result worked out on read

Status: Accepted

## Context
An auction stops taking bids at `endsAt` (006, 007), but nothing records that it ended or who won. One option is a scheduled job that writes `CLOSED` and the outcome to storage. That job would race with late bids, would need batching and idempotency, and would add a delay of up to one interval before a result appears. The kata's MVP does not ask for any of this.

## Decision
- An auction's phase is `CLOSED` when `endsAt <= now` (injected `Clock`), otherwise `OPEN`. This is the rule `placeBid` already uses. The phase is derived and never persisted. The stored `AuctionStatus` stays `OPEN`.
- Outcome (`SOLD` or `NO_BIDS`), winner and final price are worked out on each read from the stored bid totals (`bidCount`, `highestBidderId`, `currentBid`).
- No scheduled job, no migration and no `auction-closed` event. Clients refetch when `endsAt` passes. This supersedes the 011 parts of ADR 0015.

## Consequences
- Reads and bids use the same rule and the same clock. They agree at any one instant, but not across a transaction: a bid checked just before `endsAt` can commit just after it. A read in that window can show the previous leader as the winner. The window is accepted.
- The stored `status` column stays `OPEN` for ended auctions. Any query that needs closed auctions must filter on `endsAt`, as 005 does.
- A later feature that has to act when an auction ends (notifying the seller, payment) would need a job or an event. It can be added then without changing this read rule.
