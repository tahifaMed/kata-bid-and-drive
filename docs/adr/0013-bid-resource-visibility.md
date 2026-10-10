# 0013: A single bid is visible only to its bidder

Status: Accepted

## Context
007 returns `_links.self` and a `Location` header for a placed bid, so `GET /api/auctions/{id}/bids/{bidId}` must exist. The bid body carries `bidderId`. Bid ids are sequential, so a public endpoint lets anyone walk them and link bidders to amounts. 006 hides bidder ids in the public history and shows only masked names.

## Decision
- The single-bid `GET` requires `X-User-Id` like `POST`. A missing or unknown id is 401.
- Only the bidder who placed the bid can read it. For anyone else, and for a bid that is not in that auction, the answer is 404, so the response does not reveal whether the bid exists.
- The public history in the auction detail is unchanged: masked names, no bidder ids.

## Consequences
- The `Location` from a `POST` always resolves for the caller who made it.
- Spec 008 ("my bids") can reuse the same rule.
- `highestBidderId` stays public on the detail (006), because the live page needs it. Only the full bid list is kept private.
