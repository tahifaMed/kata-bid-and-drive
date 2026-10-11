/** One open auction in the list (spec 005). Money is a string (ADR 0004). */
export interface AuctionListItem {
  id: number
  car: { make: string; model: string; year: number; imageUrl: string | null }
  currentBid: string
  minNextBid: string
  bidCount: number
  endsAt: string
  _links: Record<string, { href: string }>
}

/** A HAL page of open auctions. `next` and `prev` links exist only when those pages do. */
export interface AuctionListPage {
  _embedded: { auctions: AuctionListItem[] }
  page: { size: number; number: number }
  _links: Record<string, { href: string }>
}

/** Mirrors the backend create request (spec 004). Money is a string (ADR 0004). */
export interface CreateAuctionRequest {
  car: {
    make: string
    model: string
    year: number
    mileageKm: number
    description?: string
    imageUrl?: string
  }
  startingPrice: string
  minIncrement: string
  endsAt: string
}

export interface CarDetails {
  make: string
  model: string
  year: number
  mileageKm: number
  description: string | null
  imageUrl: string | null
}

/** One bid in the public history, newest first. The bidder name is masked, for example "B***" (spec 006). */
export interface AuctionBid {
  amount: string
  placedAt: string
  bidderName: string
}

/** How a closed auction ended (spec 011). */
export type AuctionOutcome = 'SOLD' | 'NO_BIDS'

/** The auction detail (spec 006). The create response has the same shape (ADR 0010). */
export interface AuctionResponse {
  id: number
  /** `CLOSED` from `endsAt` on, worked out by the server at read time (spec 011, ADR 0016). */
  status: 'OPEN' | 'CLOSED'
  car: CarDetails
  seller: { displayName: string }
  /** Lets the bid panel tell the seller apart (spec 007). */
  sellerId: number
  startingPrice: string
  minIncrement: string
  currentBid: string
  minNextBid: string
  bidCount: number
  highestBidderId: number | null
  endsAt: string
  bids: AuctionBid[]
  /** `place-bid` is present only while the auction takes bids. */
  _links: Record<string, { href: string }>
  /** The selected user's own bid, present only when they bid on this auction (spec 008). */
  myBid?: { highestAmount: string; status: 'WINNING' | 'OUTBID' | 'WON' | 'LOST' }
  /** Present only when `CLOSED` (spec 011). */
  outcome?: AuctionOutcome
  /** The masked winner, for example "B***". Present only when `SOLD`. */
  winnerName?: string
}

/** The `data` of a `bid-placed` event (spec 009). Money is a string (ADR 0004). */
export interface BidPlacedData {
  bidId: number
  amount: string
  bidderId: number
  /** Masked as in the bid history, for example "B***". */
  bidderName: string
  highestBidderId: number
  currentBid: string
  minNextBid: string
  bidCount: number
  endsAt: string
  extended: boolean
}

/** One message on the auction events stream (spec 009, ADR 0015). `occurredAt` is the bid's `placedAt`. */
export interface BidPlacedEvent {
  type: 'bid-placed'
  auctionId: number
  occurredAt: string
  data: BidPlacedData
}
