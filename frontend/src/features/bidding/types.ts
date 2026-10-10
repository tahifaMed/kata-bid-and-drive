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
