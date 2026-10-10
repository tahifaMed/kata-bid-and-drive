import { ref } from 'vue'
import { ApiError } from '../../../api/client'
import { fetchAuction } from '../api'
import type { AuctionResponse } from '../types'

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
  // Time left is measured from when the data arrived.
  const loadedAt = ref(Date.now())
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
      loadedAt.value = Date.now()
      state.value = 'ready'
    } catch (error) {
      if (request !== latestRequest) return
      state.value = error instanceof ApiError && error.status === 404 ? 'not-found' : 'error'
    }
  }

  function retry(): Promise<void> {
    return load(requestedId)
  }

  return { state, auction, loadedAt, load, retry }
}
