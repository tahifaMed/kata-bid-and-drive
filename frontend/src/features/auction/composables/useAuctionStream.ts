import { onScopeDispose, ref, watch, type Ref } from 'vue'
import { auctionEventsUrl } from '../api'
import type { BidPlacedEvent } from '../types'

/** Nothing is shown while `connecting`; the page says "Live", "Reconnecting" or "Live updates unavailable" (spec 010). */
export type AuctionStreamStatus = 'connecting' | 'live' | 'reconnecting' | 'unavailable'

export interface AuctionStreamHandlers {
  onBidPlaced(event: BidPlacedEvent): void
  /** The stream is back after an error. Events sent meanwhile are lost, so the page fetches the auction again. */
  onReconnected(): void
}

/** Errors in a row, with no successful open between them, before the page stops trying. */
export const MAX_ERRORS = 5
/** Wait before opening a new stream once the browser gave up on the old one. */
export const REOPEN_DELAY_MS = 3000

function isBidPlacedFor(value: unknown, auctionId: number): value is BidPlacedEvent {
  return (
    typeof value === 'object' &&
    value !== null &&
    'type' in value &&
    value.type === 'bid-placed' &&
    'auctionId' in value &&
    value.auctionId === auctionId &&
    'data' in value &&
    typeof value.data === 'object' &&
    value.data !== null
  )
}

/** Listens to the live events of one auction, reconnecting on errors up to a limit (spec 010). */
export function useAuctionStream(auctionId: Ref<number | null>, handlers: AuctionStreamHandlers) {
  const status = ref<AuctionStreamStatus>('connecting')
  let source: EventSource | null = null
  let reopenTimer: ReturnType<typeof setTimeout> | undefined
  let errors = 0
  // Set by an error, so the next open knows events may have been missed.
  let missedEvents = false

  function stop(): void {
    clearTimeout(reopenTimer)
    reopenTimer = undefined
    source?.close()
    source = null
  }

  function open(id: number): void {
    const opened = new EventSource(auctionEventsUrl(id))
    source = opened

    opened.onopen = () => {
      errors = 0
      status.value = 'live'
      if (missedEvents) {
        missedEvents = false
        handlers.onReconnected()
      }
    }

    opened.onerror = () => {
      errors += 1
      missedEvents = true
      if (errors >= MAX_ERRORS) {
        stop()
        status.value = 'unavailable'
        return
      }
      status.value = 'reconnecting'
      // While CONNECTING the browser retries by itself. Once CLOSED it never will, so a new stream is opened.
      if (opened.readyState === EventSource.CLOSED) {
        opened.close()
        reopenTimer = setTimeout(() => open(id), REOPEN_DELAY_MS)
      }
    }

    // The server is the source of truth, but a message is checked before it may change the page.
    opened.addEventListener('bid-placed', (message: MessageEvent<string>) => {
      let event: unknown
      try {
        event = JSON.parse(message.data)
      } catch {
        return
      }
      if (isBidPlacedFor(event, id)) handlers.onBidPlaced(event)
    })
  }

  watch(
    auctionId,
    (id) => {
      stop()
      errors = 0
      missedEvents = false
      status.value = 'connecting'
      if (id !== null) open(id)
    },
    { immediate: true },
  )

  onScopeDispose(stop)

  return { status }
}
