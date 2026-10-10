export interface PhotoCredit {
  author: string
  license: string
  url: string
}

export interface AuctionListing {
  id: string
  title: string
  specs: string
  location: string
  timeLeft: string
  currentBid: number
  noReserve: boolean
  image: string
  imageAlt: string
  credit: PhotoCredit
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

export interface AuctionResponse {
  id: number
  status: 'OPEN'
  car: CarDetails
  seller: { displayName: string }
  startingPrice: string
  minIncrement: string
  currentBid: string
  minNextBid: string
  bidCount: number
  highestBidderId: number | null
  endsAt: string
  bids: never[]
  _links: Record<string, { href: string }>
}
