export interface PlaceBidRequest {
  /** Money is a string with two decimals (ADR 0004). */
  amount: string
}

/** A placed bid, as `POST /api/auctions/{id}/bids` returns it (spec 007). */
export interface BidResponse {
  id: number
  auctionId: number
  bidderId: number
  amount: string
  placedAt: string
  _links: Record<string, { href: string }>
}

/** Where the caller stands on an auction they bid on: WINNING or OUTBID while open (spec 008), WON or LOST once closed (spec 011). */
export type BidderStatus = 'WINNING' | 'OUTBID' | 'WON' | 'LOST'

/** One auction the caller bid on, as `GET /api/me/bids` returns it. Money is a string (ADR 0004). */
export interface MyBid {
  auctionId: number
  car: { make: string; model: string; year: number; imageUrl: string | null }
  myHighestBid: string
  currentBid: string
  status: BidderStatus
  endsAt: string
  /** `auction` links to the auction the bid was placed on. */
  _links: Record<string, { href: string }>
}

/** The caller's bids, sorted by end time. Not paged (spec 008). */
export interface MyBidsCollection {
  _embedded: { myBids: MyBid[] }
  _links: Record<string, { href: string }>
}
