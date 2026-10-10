import { ref } from 'vue'
import { ApiError } from '../../../api/client'
import { fetchAuction } from '../api'
import type { AuctionResponse } from '../types'

export type AuctionDetailState = 'loading' | 'ready' | 'not-found' | 'error'

const WHOLE_NUMBER = /^\d+$/

/** Fetches one auction and tracks the loading, not-found and error states (spec 006). */
export function useAuctionDetail() {
  const state = ref<AuctionDetailState>('loading')
  const auction = ref<AuctionResponse | null>(null)
  // Time left is measured from when the data arrived.
  const loadedAt = ref(Date.now())
  // The id last asked for, so a retry repeats the failed request.
  let requestedId = ''

  async function load(id: string): Promise<void> {
    requestedId = id
    // A route param that is not an id cannot match an auction, so it is not sent to the API.
    if (!WHOLE_NUMBER.test(id)) {
      auction.value = null
      state.value = 'not-found'
      return
    }
    state.value = 'loading'
    try {
      auction.value = await fetchAuction(Number(id))
      loadedAt.value = Date.now()
      state.value = 'ready'
    } catch (error) {
      state.value = error instanceof ApiError && error.status === 404 ? 'not-found' : 'error'
    }
  }

  function retry(): Promise<void> {
    return load(requestedId)
  }

  return { state, auction, loadedAt, load, retry }
}
