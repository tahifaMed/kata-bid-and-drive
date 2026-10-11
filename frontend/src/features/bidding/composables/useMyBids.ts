import { ref } from 'vue'
import { fetchMyBids } from '../api'
import type { MyBid } from '../types'

export type MyBidsState = 'loading' | 'ready' | 'error'

/** Fetches the selected user's bids and tracks the loading and error states (spec 008). */
export function useMyBids() {
  const state = ref<MyBidsState>('loading')
  const items = ref<MyBid[]>([])
  // Time left is measured from when the data arrived.
  const loadedAt = ref(Date.now())
  // Only the latest request may update the state, so a slow answer for the previous user is dropped.
  let latestRequest = 0

  async function load(): Promise<void> {
    const request = ++latestRequest
    state.value = 'loading'
    try {
      const result = await fetchMyBids()
      if (request !== latestRequest) return
      items.value = result._embedded.myBids
      loadedAt.value = Date.now()
      state.value = 'ready'
    } catch {
      if (request !== latestRequest) return
      state.value = 'error'
    }
  }

  return { state, items, loadedAt, load }
}
