import { effectScope, nextTick, ref } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { bidPlaced, FakeEventSource } from '../testing/FakeEventSource'
import type { BidPlacedEvent } from '../types'
import { useAuctionStream } from './useAuctionStream'

beforeEach(() => {
  FakeEventSource.reset()
  vi.stubGlobal('EventSource', FakeEventSource)
  vi.useFakeTimers()
})
afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

function setup(id: number | null = 5) {
  const auctionId = ref(id)
  const onBidPlaced = vi.fn<(event: BidPlacedEvent) => void>()
  const onReconnected = vi.fn<() => void>()
  const scope = effectScope()
  const stream = scope.run(() => useAuctionStream(auctionId, { onBidPlaced, onReconnected }))!
  return { scope, auctionId, onBidPlaced, onReconnected, ...stream }
}

describe('useAuctionStream', () => {
  it('opens the events stream of the auction', () => {
    setup(5)

    expect(FakeEventSource.latest().url).toBe('/api/auctions/5/events')
  })

  it('opens nothing while there is no auction id', () => {
    setup(null)

    expect(FakeEventSource.instances).toHaveLength(0)
  })

  it('is connecting until the stream opens, then live', () => {
    const { status } = setup()
    expect(status.value).toBe('connecting')

    FakeEventSource.latest().open()

    expect(status.value).toBe('live')
  })

  it('passes a bid-placed event for the auction to the handler', () => {
    const { onBidPlaced } = setup()
    const event = bidPlaced()

    FakeEventSource.latest().emit(event)

    expect(onBidPlaced).toHaveBeenCalledWith(event)
  })

  it('ignores an event for another auction', () => {
    const { onBidPlaced } = setup(5)

    FakeEventSource.latest().emit(bidPlaced({}, { auctionId: 6 }))

    expect(onBidPlaced).not.toHaveBeenCalled()
  })

  it('ignores a message whose type does not match its event name', () => {
    const { onBidPlaced } = setup()

    FakeEventSource.latest().emitRaw(
      'bid-placed',
      JSON.stringify({ ...bidPlaced(), type: 'auction-closed' }),
    )

    expect(onBidPlaced).not.toHaveBeenCalled()
  })

  it('ignores a message that is not JSON', () => {
    const { onBidPlaced } = setup()

    FakeEventSource.latest().emitRaw('bid-placed', 'not json')

    expect(onBidPlaced).not.toHaveBeenCalled()
  })

  it('closes the stream when its scope is disposed, such as on leaving the page', () => {
    const { scope } = setup()
    const source = FakeEventSource.latest()

    scope.stop()

    expect(source.closed).toBe(true)
  })

  it('closes the old stream and opens a new one when the auction id changes', async () => {
    const { auctionId } = setup(5)
    const first = FakeEventSource.latest()

    auctionId.value = 6
    await nextTick()

    expect(first.closed).toBe(true)
    expect(FakeEventSource.latest().url).toBe('/api/auctions/6/events')
  })

  it('shows reconnecting on an error and lets the browser retry', () => {
    const { status } = setup()
    const source = FakeEventSource.latest()
    source.open()

    source.fail()

    expect(status.value).toBe('reconnecting')
    expect(source.closed).toBe(false)
    expect(FakeEventSource.instances).toHaveLength(1)
  })

  it('refetches when the stream opens again after an error, not on the first open', () => {
    const { onReconnected, status } = setup()
    const source = FakeEventSource.latest()
    source.open()
    expect(onReconnected).not.toHaveBeenCalled()

    source.fail()
    source.open()

    expect(onReconnected).toHaveBeenCalledTimes(1)
    expect(status.value).toBe('live')
  })

  it('opens a new stream after a delay when the browser gave up on the old one', () => {
    setup()
    FakeEventSource.latest().fail(FakeEventSource.CLOSED)
    expect(FakeEventSource.instances).toHaveLength(1)

    vi.advanceTimersByTime(3000)

    expect(FakeEventSource.instances).toHaveLength(2)
    expect(FakeEventSource.latest().url).toBe('/api/auctions/5/events')
  })

  it('stops retrying after 5 errors in a row and says live updates are unavailable', () => {
    const { status } = setup()
    const source = FakeEventSource.latest()

    for (let i = 0; i < 5; i++) source.fail()

    expect(status.value).toBe('unavailable')
    expect(source.closed).toBe(true)
  })

  it('does not reopen a stream once it stopped', () => {
    const { status } = setup()
    for (let i = 0; i < 4; i++) {
      FakeEventSource.latest().fail(FakeEventSource.CLOSED)
      vi.advanceTimersByTime(3000)
    }
    FakeEventSource.latest().fail(FakeEventSource.CLOSED)
    expect(FakeEventSource.instances).toHaveLength(5)

    vi.advanceTimersByTime(30_000)

    expect(FakeEventSource.instances).toHaveLength(5)
    expect(status.value).toBe('unavailable')
  })

  it('keeps a single reopen pending when the old stream reports more errors', () => {
    setup()
    const source = FakeEventSource.latest()
    source.fail(FakeEventSource.CLOSED)
    source.fail(FakeEventSource.CLOSED)

    vi.advanceTimersByTime(3000)

    expect(FakeEventSource.instances).toHaveLength(2)
  })

  it('ignores errors and opens from a stream it already replaced', () => {
    const { status } = setup()
    const old = FakeEventSource.latest()
    old.fail(FakeEventSource.CLOSED)
    vi.advanceTimersByTime(3000)
    FakeEventSource.latest().open()

    old.fail(FakeEventSource.CLOSED)

    expect(status.value).toBe('live')
    vi.advanceTimersByTime(3000)
    expect(FakeEventSource.instances).toHaveLength(2)
  })

  it('counts errors across reopened streams', () => {
    const { status } = setup()
    for (let i = 0; i < 4; i++) {
      FakeEventSource.latest().fail(FakeEventSource.CLOSED)
      vi.advanceTimersByTime(3000)
    }
    expect(status.value).toBe('reconnecting')

    FakeEventSource.latest().fail(FakeEventSource.CLOSED)

    expect(status.value).toBe('unavailable')
  })

  it('starts counting again after a successful open', () => {
    const { status } = setup()
    const source = FakeEventSource.latest()
    for (let i = 0; i < 4; i++) source.fail()
    source.open()

    for (let i = 0; i < 4; i++) source.fail()

    expect(status.value).toBe('reconnecting')
  })

  it('cancels a pending reopen when its scope is disposed', () => {
    const { scope } = setup()
    FakeEventSource.latest().fail(FakeEventSource.CLOSED)

    scope.stop()
    vi.advanceTimersByTime(3000)

    expect(FakeEventSource.instances).toHaveLength(1)
  })
})
