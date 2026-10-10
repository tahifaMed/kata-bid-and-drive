# 0003: Optimistic locking for concurrent bids

Status: Accepted

## Context
Several buyers may bid on the same car at the same moment. Two bids that both read the same current price must not both be accepted.

## Options considered
- **Pessimistic lock** (`SELECT ... FOR UPDATE` on the auction row): correct, but blocks readers and bidders for the length of the transaction.
- **Optimistic lock** (`@Version` on the auction): no blocking, and conflicts are detected at commit.
- **Serializable isolation**: correct, but database-dependent and noisy under contention.

## Decision
Use `@Version` on the auction. A bid writes the new auction state and the bid row in one transaction. A version conflict returns 409 with a message to reload and retry. The server does not retry automatically, so the bidder always sees the latest price before deciding again.

## Consequences
- Losers of a race get a 409, not a silent retry. The bid panel refreshes the auction.
- A parallel-bid integration test is required in spec 007 to prove that exactly one bid wins per amount.
