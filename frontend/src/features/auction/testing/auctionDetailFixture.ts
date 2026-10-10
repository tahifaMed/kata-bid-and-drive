import type { AuctionResponse } from '../types'

/** An open auction detail as the API returns it, with no bids. Override any field per test. */
export function auctionDetail(overrides: Partial<AuctionResponse> = {}): AuctionResponse {
  return {
    id: 5,
    status: 'OPEN',
    car: {
      make: 'Volkswagen',
      model: 'Golf',
      year: 2020,
      mileageKm: 42000,
      description: 'Blue, one owner',
      imageUrl: 'https://example.com/golf.jpg',
    },
    seller: { displayName: 'Alice' },
    sellerId: 1,
    startingPrice: '12000.00',
    minIncrement: '100.00',
    currentBid: '12000.00',
    minNextBid: '12000.00',
    bidCount: 0,
    highestBidderId: null,
    endsAt: '2026-10-12T18:00:00Z',
    bids: [],
    _links: {
      self: { href: 'http://localhost/api/auctions/5' },
      'place-bid': { href: 'http://localhost/api/auctions/5/bids' },
    },
    ...overrides,
  }
}
