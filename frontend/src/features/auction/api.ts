import { apiFetch } from '../../api/client'
import type { AuctionResponse, CreateAuctionRequest } from './types'

export function createAuction(request: CreateAuctionRequest): Promise<AuctionResponse> {
  return apiFetch<AuctionResponse>('/api/auctions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
}
