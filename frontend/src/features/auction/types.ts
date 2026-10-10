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

/** The auction detail (spec 006). The create response has the same shape (ADR 0010). */
export interface AuctionResponse {
  id: number
  status: 'OPEN'
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
}
