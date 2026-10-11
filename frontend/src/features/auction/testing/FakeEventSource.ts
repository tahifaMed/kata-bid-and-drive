import type { BidPlacedEvent } from '../types'

type Listener = (event: MessageEvent<string>) => void

/** A stand-in for the browser `EventSource`, which jsdom lacks. Install it with `vi.stubGlobal('EventSource', FakeEventSource)`. */
export class FakeEventSource {
  static readonly CONNECTING = 0
  static readonly OPEN = 1
  static readonly CLOSED = 2

  /** Every source opened since the last `reset()`, oldest first. */
  static instances: FakeEventSource[] = []

  static reset(): void {
    FakeEventSource.instances = []
  }

  /** The source opened last. */
  static latest(): FakeEventSource {
    const source = FakeEventSource.instances.at(-1)
    if (!source) throw new Error('No EventSource was opened')
    return source
  }

  readyState = FakeEventSource.CONNECTING
  onopen: (() => void) | null = null
  onerror: (() => void) | null = null
  private readonly listeners = new Map<string, Listener[]>()

  constructor(readonly url: string) {
    FakeEventSource.instances.push(this)
  }

  addEventListener(type: string, listener: Listener): void {
    this.listeners.set(type, [...(this.listeners.get(type) ?? []), listener])
  }

  close(): void {
    this.readyState = FakeEventSource.CLOSED
  }

  get closed(): boolean {
    return this.readyState === FakeEventSource.CLOSED
  }

  /** The server accepted the stream. */
  open(): void {
    this.readyState = FakeEventSource.OPEN
    this.onopen?.()
  }

  /** The connection failed. The browser retries by itself while the state is CONNECTING, and gives up when it is CLOSED. */
  fail(readyState: number = FakeEventSource.CONNECTING): void {
    this.readyState = readyState
    this.onerror?.()
  }

  /** Delivers one SSE message with the raw `data:` text. */
  emitRaw(type: string, data: string): void {
    for (const listener of this.listeners.get(type) ?? []) {
      listener(new MessageEvent(type, { data }))
    }
  }

  emit(event: BidPlacedEvent): void {
    this.emitRaw(event.type, JSON.stringify(event))
  }
}

/** A `bid-placed` event for auction 5 by user 2. Override any field per test. */
export function bidPlaced(
  data: Partial<BidPlacedEvent['data']> = {},
  envelope: Partial<Omit<BidPlacedEvent, 'data'>> = {},
): BidPlacedEvent {
  return {
    type: 'bid-placed',
    auctionId: 5,
    occurredAt: '2026-10-11T10:00:00Z',
    ...envelope,
    data: {
      bidId: 42,
      amount: '12000.00',
      bidderId: 2,
      bidderName: 'B***',
      highestBidderId: 2,
      currentBid: '12000.00',
      minNextBid: '12100.00',
      bidCount: 1,
      endsAt: '2026-10-12T18:00:00Z',
      extended: false,
      ...data,
    },
  }
}
