import { ref } from 'vue'
import { ApiError } from '../../../api/client'
import { fetchAuction } from '../api'
import type { AuctionResponse, BidPlacedEvent } from '../types'

export type AuctionDetailState = 'loading' | 'ready' | 'not-found' | 'error'

const WHOLE_NUMBER = /^\d+$/

/** The auction id in a route param, or null when it is not a whole number JavaScript can hold exactly. */
function parseId(param: string): number | null {
  const id = Number(param)
  return WHOLE_NUMBER.test(param) && Number.isSafeInteger(id) ? id : null
}

/** Fetches one auction and tracks the loading, not-found and error states (spec 006). */
export function useAuctionDetail() {
  const state = ref<AuctionDetailState>('loading')
  const auction = ref<AuctionResponse | null>(null)
  // The id last asked for, so a retry repeats the failed request.
  let requestedId = ''
  // Only the latest request may update the state, so a slow answer for an old id is dropped.
  let latestRequest = 0

  async function load(param: string): Promise<void> {
    requestedId = param
    const request = ++latestRequest
    // A route param that is not an id cannot match an auction, so it is not sent to the API.
    const id = parseId(param)
    if (id === null) {
      auction.value = null
      state.value = 'not-found'
      return
    }
    state.value = 'loading'
    try {
      const result = await fetchAuction(id)
      if (request !== latestRequest) return
      auction.value = result
      state.value = 'ready'
    } catch (error) {
      if (request !== latestRequest) return
      state.value = error instanceof ApiError && error.status === 404 ? 'not-found' : 'error'
    }
  }

  function retry(): Promise<void> {
    return load(requestedId)
  }

  /** Fetches the shown auction again, without the loading state, so the page stays on screen (spec 007). */
  async function refresh(): Promise<void> {
    const id = parseId(requestedId)
    if (id === null || auction.value === null) return
    const request = ++latestRequest
    try {
      const result = await fetchAuction(id)
      if (request !== latestRequest) return
      // A live event may have arrived while this was on its way; fewer bids means older data.
      if (auction.value !== null && result.bidCount < auction.value.bidCount) return
      auction.value = result
    } catch {
      // The last auction stays shown. The bid panel has already said what went wrong.
    }
  }

  /**
   * Applies a live bid to the shown auction (spec 010). An event for another auction, or one not newer than
   * what is shown (a bid count not above the shown one), is ignored. An event that skips a count also fetches
   * the auction again. Returns whether the event was applied.
   */
  function applyBidPlaced(event: BidPlacedEvent): boolean {
    const shown = auction.value
    if (shown === null || event.auctionId !== shown.id || event.data.bidCount <= shown.bidCount) {
      return false
    }
    const { amount, bidderName, currentBid, minNextBid, bidCount, highestBidderId, endsAt } = event.data
    auction.value = {
      ...shown,
      currentBid,
      minNextBid,
      bidCount,
      highestBidderId,
      endsAt,
      bids: [{ amount, placedAt: event.occurredAt, bidderName }, ...shown.bids],
    }
    // A jump in the count means events were lost, so the missing history rows come from the server.
    if (bidCount > shown.bidCount + 1) void refresh()
    return true
  }

  return { state, auction, load, retry, refresh, applyBidPlaced }
}
