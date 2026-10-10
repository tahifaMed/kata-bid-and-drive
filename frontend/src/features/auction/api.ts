import { apiFetch } from '../../api/client'
import type { AuctionListPage, AuctionResponse, CreateAuctionRequest } from './types'

const PAGE_SIZE = 20

export function createAuction(request: CreateAuctionRequest): Promise<AuctionResponse> {
  return apiFetch<AuctionResponse>('/api/auctions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
}

/** One auction with its bid history (spec 006). Rejects with a 404 `ApiError` for an unknown id. */
export function fetchAuction(id: number): Promise<AuctionResponse> {
  return apiFetch<AuctionResponse>(`/api/auctions/${id}`)
}

/** One page of open auctions, 0-based (spec 005). */
export function fetchOpenAuctions(page: number): Promise<AuctionListPage> {
  return apiFetch<AuctionListPage>(`/api/auctions?page=${page}&size=${PAGE_SIZE}`)
}
