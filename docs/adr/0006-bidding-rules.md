# 0006: Bidding rules: ownership, self-outbid and first bid

Status: Accepted

## Context
The kata is silent on whether a seller may bid on their own car, whether a bidder may raise their own price, and what the first bid must be. Without a rule, the auction can be inflated by its owner or by one bidder raising against themselves.

## Decision
- **Ownership**: the seller of an auction cannot bid on it. This is checked against the auction's seller, not a stored role. Any user may still be a seller of their own cars.
- **No self-outbid**: a bidder who is the current highest bidder cannot bid again until someone else has bid.
- **First bid**: when there are no bids, the minimum is the starting price, so the first bid may equal it. Later bids must be at least the current bid plus the minimum increment.
- Bids are immutable and there is no proxy bidding.

## Consequences
- Sellers cannot inflate their own auction.
- Bidders cannot drive the price up against themselves. A duplicate submission is rejected as "already highest".
- Proxy (maximum) bidding would change the "no self-outbid" rule and needs its own ADR if added.
