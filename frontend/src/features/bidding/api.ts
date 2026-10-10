import { apiFetch } from '../../api/client'
import type { BidResponse, PlaceBidRequest } from './types'

/**
 * Posts a bid to the auction's `place-bid` link. Only the path is used, so the request stays on the page's
 * origin, through the dev proxy, whatever host the server put in the link.
 */
export function placeBid(placeBidHref: string, amount: string): Promise<BidResponse> {
  const request: PlaceBidRequest = { amount }
  return apiFetch<BidResponse>(new URL(placeBidHref).pathname, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
}
