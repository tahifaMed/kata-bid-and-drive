import { createPinia, type Pinia } from 'pinia'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/vue'
import { flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../api/client'
import { placeBid } from '../../bidding/api'
import { useIdentityStore } from '../../identity/stores/identity'
import { fetchAuction } from '../api'
import { auctionDetail } from '../testing/auctionDetailFixture'
import { bidPlaced, FakeEventSource } from '../testing/FakeEventSource'
import AuctionDetailView from './AuctionDetailView.vue'

vi.mock('../api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../api')>()),
  fetchAuction: vi.fn<typeof fetchAuction>(),
}))
vi.mock('../../bidding/api', () => ({ placeBid: vi.fn<typeof placeBid>() }))

afterEach(() => {
  cleanup()
  vi.useRealTimers()
  vi.unstubAllGlobals()
})
beforeEach(() => {
  FakeEventSource.reset()
  vi.stubGlobal('EventSource', FakeEventSource)
  vi.mocked(fetchAuction).mockReset()
  vi.mocked(placeBid).mockReset()
  sessionStorage.clear()
})

let pinia: Pinia

async function renderAt(path: string, userId = 1) {
  sessionStorage.setItem('bidanddrive.currentUserId', String(userId))
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { render: () => null } },
      { path: '/auctions/:id', component: AuctionDetailView },
    ],
  })
  await router.push(path)
  await router.isReady()
  pinia = createPinia()
  render(AuctionDetailView, { global: { plugins: [pinia, router] } })
  return router
}

describe('AuctionDetailView', () => {
  it('shows a loading message while the auction is on its way', async () => {
    vi.mocked(fetchAuction).mockReturnValue(new Promise(() => {}))
    await renderAt('/auctions/5')

    expect(screen.getByRole('status').textContent).toContain('Loading auction…')
  })

  it('shows the car, the price block, the bid history and a link back to the list', async () => {
    vi.mocked(fetchAuction).mockResolvedValue(
      auctionDetail({
        bids: [{ amount: '12000.00', placedAt: '2026-10-11T10:00:00Z', bidderName: 'B***' }],
      }),
    )
    await renderAt('/auctions/5')

    expect(
      await screen.findByRole('heading', { level: 1, name: '2020 Volkswagen Golf' }),
    ).toBeTruthy()
    expect(fetchAuction).toHaveBeenCalledWith(5)
    expect(screen.getByRole('region', { name: 'Price' })).toBeTruthy()
    expect(screen.getByRole('region', { name: 'Bid history' })).toBeTruthy()
    expect(screen.getByText('B***')).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Back to auctions' }).getAttribute('href')).toBe(
      '/',
    )
  })

  it('shows the bid panel while the auction takes bids', async () => {
    vi.mocked(fetchAuction).mockResolvedValue(auctionDetail())
    await renderAt('/auctions/5', 2)

    expect(await screen.findByRole('region', { name: 'Place a bid' })).toBeTruthy()
    expect(screen.getByLabelText('Your bid (USD)')).toBeTruthy()
  })

  it('refreshes the auction after a bid, keeping the page on screen', async () => {
    vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail())
    let answer: (auction: ReturnType<typeof auctionDetail>) => void = () => {}
    vi.mocked(fetchAuction).mockReturnValueOnce(new Promise((resolve) => (answer = resolve)))
    vi.mocked(placeBid).mockResolvedValue({
      id: 12,
      auctionId: 5,
      bidderId: 2,
      amount: '12000.00',
      placedAt: '2026-10-11T10:00:00Z',
      _links: {},
    })
    await renderAt('/auctions/5', 2)

    await fireEvent.update(await screen.findByLabelText('Your bid (USD)'), '12000.00')
    await fireEvent.click(screen.getByRole('button', { name: 'Place bid' }))
    await screen.findByText('Your bid of USD 12,000.00 was placed.')

    expect(fetchAuction).toHaveBeenCalledTimes(2)
    expect(screen.queryByText('Loading auction…')).toBeNull()
    expect(screen.getByRole('region', { name: 'Place a bid' })).toBeTruthy()

    answer(auctionDetail({ currentBid: '12000.00', minNextBid: '12100.00', bidCount: 1, highestBidderId: 2 }))

    expect(await screen.findByText('You are the highest bidder.')).toBeTruthy()
    expect(screen.getByText('USD 12,100.00')).toBeTruthy()
  })

  it('hides the bid panel when the auction has no place-bid link', async () => {
    vi.mocked(fetchAuction).mockResolvedValue(
      auctionDetail({ _links: { self: { href: 'http://localhost/api/auctions/5' } } }),
    )
    await renderAt('/auctions/5')

    await screen.findByRole('heading', { level: 1 })
    expect(screen.queryByRole('region', { name: 'Place a bid' })).toBeNull()
  })

  it('says the auction was not found on a 404, with a link back to the list', async () => {
    vi.mocked(fetchAuction).mockRejectedValue(new ApiError(404, 'Not found'))
    await renderAt('/auctions/99')

    expect(await screen.findByText('This auction was not found.')).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Back to auctions' })).toBeTruthy()
  })

  it('offers a retry when the auction could not be loaded', async () => {
    vi.mocked(fetchAuction).mockRejectedValueOnce(new ApiError(500, 'Server error'))
    vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail())
    await renderAt('/auctions/5')

    expect((await screen.findByRole('alert')).textContent).toContain('Could not load this auction.')
    await fireEvent.click(screen.getByRole('button', { name: 'Try again' }))

    expect(
      await screen.findByRole('heading', { level: 1, name: '2020 Volkswagen Golf' }),
    ).toBeTruthy()
  })

  it('loads the other auction when the route id changes', async () => {
    vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail())
    vi.mocked(fetchAuction).mockResolvedValueOnce(
      auctionDetail({ id: 6, car: { ...auctionDetail().car, make: 'Peugeot', model: '208' } }),
    )
    const router = await renderAt('/auctions/5')
    await screen.findByRole('heading', { level: 1, name: '2020 Volkswagen Golf' })

    await router.push('/auctions/6')

    expect(await screen.findByRole('heading', { level: 1, name: '2020 Peugeot 208' })).toBeTruthy()
    expect(fetchAuction).toHaveBeenLastCalledWith(6)
  })

  describe('live updates', () => {
    it('updates the price, count, minimum and history when another user bids', async () => {
      vi.mocked(fetchAuction).mockResolvedValue(auctionDetail())
      await renderAt('/auctions/5', 3)
      const price = await screen.findByRole('region', { name: 'Price' })
      expect(FakeEventSource.latest().url).toBe('/api/auctions/5/events')

      FakeEventSource.latest().emit(
        bidPlaced({
          amount: '12500.00',
          bidderName: 'B***',
          currentBid: '12500.00',
          minNextBid: '12600.00',
          bidCount: 1,
        }),
      )

      expect(await within(price).findByText('USD 12,500.00')).toBeTruthy()
      expect(within(price).getByText('USD 12,600.00')).toBeTruthy()
      expect(within(price).getByText('1')).toBeTruthy()
      expect(screen.getByText('Minimum bid: USD 12,600.00')).toBeTruthy()
      const history = screen.getByRole('region', { name: 'Bid history' })
      expect(within(history).getByText('USD 12,500.00')).toBeTruthy()
      expect(within(history).getByText('B***')).toBeTruthy()
      expect(fetchAuction).toHaveBeenCalledTimes(1)
    })

    it('fetches the auction again when the stream comes back after an error', async () => {
      vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail())
      vi.mocked(fetchAuction).mockResolvedValueOnce(
        auctionDetail({ currentBid: '13000.00', minNextBid: '13100.00', bidCount: 2 }),
      )
      await renderAt('/auctions/5', 3)
      await screen.findByRole('region', { name: 'Price' })
      const source = FakeEventSource.latest()
      source.open()
      expect(fetchAuction).toHaveBeenCalledTimes(1)

      source.fail()
      source.open()

      expect(await screen.findByText('USD 13,000.00')).toBeTruthy()
      expect(fetchAuction).toHaveBeenCalledTimes(2)
    })

    it('counts the time left down to the end time the event brings', async () => {
      vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval'] })
      vi.setSystemTime(new Date('2026-10-11T12:00:00Z'))
      vi.mocked(fetchAuction).mockResolvedValue(auctionDetail({ endsAt: '2026-10-11T15:00:00Z' }))
      await renderAt('/auctions/5', 3)
      expect(await screen.findByText('3h 0m')).toBeTruthy()

      vi.advanceTimersByTime(60_000)
      expect(await screen.findByText('2h 59m')).toBeTruthy()

      FakeEventSource.latest().emit(bidPlaced({ endsAt: '2026-10-11T16:30:00Z' }))

      expect(await screen.findByText('4h 29m')).toBeTruthy()
    })

    it('closes the stream when the user leaves the page', async () => {
      vi.mocked(fetchAuction).mockResolvedValue(auctionDetail())
      await renderAt('/auctions/5')
      await screen.findByRole('region', { name: 'Price' })
      const source = FakeEventSource.latest()

      cleanup()

      expect(source.closed).toBe(true)
    })

    it('listens to the other auction when the route id changes', async () => {
      vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail())
      vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail({ id: 6 }))
      const router = await renderAt('/auctions/5')
      await screen.findByRole('region', { name: 'Price' })
      const first = FakeEventSource.latest()

      await router.push('/auctions/6')

      await vi.waitFor(() => expect(FakeEventSource.latest().url).toBe('/api/auctions/6/events'))
      expect(first.closed).toBe(true)
    })

    it('opens no stream for an auction that was not found', async () => {
      vi.mocked(fetchAuction).mockRejectedValue(new ApiError(404, 'Not found'))
      await renderAt('/auctions/99')
      await screen.findByText('This auction was not found.')

      expect(FakeEventSource.instances).toHaveLength(0)
    })
  })

  describe('auction result', () => {
    const SELF = { self: { href: 'http://localhost/api/auctions/5' } }

    function closed(overrides: Parameters<typeof auctionDetail>[0] = {}) {
      return auctionDetail({
        status: 'CLOSED',
        outcome: 'SOLD',
        winnerName: 'B***',
        currentBid: '5200.00',
        bidCount: 3,
        highestBidderId: 2,
        endsAt: '2026-10-11T12:00:00Z',
        _links: SELF,
        ...overrides,
      })
    }

    it.each([
      ['the winner', 2],
      ['the seller', 1],
      ['another buyer', 3],
    ])('shows %s who won at what price, and no bid panel or minimum, on a sold auction', async (_, userId) => {
      vi.mocked(fetchAuction).mockResolvedValue(closed())
      await renderAt('/auctions/5', userId)

      const result = await screen.findByRole('region', { name: 'Result' })
      expect(within(result).getByText('Sold to B*** for USD 5,200.00')).toBeTruthy()
      expect(screen.getByText('Final price')).toBeTruthy()
      expect(screen.queryByText('Minimum next bid')).toBeNull()
      expect(screen.queryByRole('region', { name: 'Place a bid' })).toBeNull()
    })

    it('says no bids on a closed auction nobody bid on', async () => {
      vi.mocked(fetchAuction).mockResolvedValue(
        closed({ outcome: 'NO_BIDS', winnerName: undefined, currentBid: '12000.00', bidCount: 0, highestBidderId: null }),
      )
      await renderAt('/auctions/5', 1)

      const result = await screen.findByRole('region', { name: 'Result' })
      expect(within(result).getByText('No bids')).toBeTruthy()
      expect(screen.getByText('Starting price')).toBeTruthy()
    })

    it('shows no result while the auction is open', async () => {
      vi.mocked(fetchAuction).mockResolvedValue(auctionDetail())
      await renderAt('/auctions/5', 2)
      await screen.findByRole('region', { name: 'Place a bid' })

      expect(screen.queryByRole('region', { name: 'Result' })).toBeNull()
    })

    describe('when the countdown reaches the end', () => {
      beforeEach(() => {
        vi.useFakeTimers({ toFake: ['Date', 'setInterval', 'clearInterval', 'setTimeout', 'clearTimeout'] })
        vi.setSystemTime(new Date('2026-10-11T11:59:58Z'))
      })

      it('fetches the auction and shows the result', async () => {
        vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail({ endsAt: '2026-10-11T12:00:00Z' }))
        vi.mocked(fetchAuction).mockResolvedValueOnce(closed())
        await renderAt('/auctions/5', 3)
        await screen.findByRole('region', { name: 'Place a bid' })

        await vi.advanceTimersByTimeAsync(2_000)

        expect(await screen.findByText('Sold to B*** for USD 5,200.00')).toBeTruthy()
        expect(screen.queryByRole('region', { name: 'Place a bid' })).toBeNull()
        expect(fetchAuction).toHaveBeenCalledTimes(2)
      })

      it('hides the bid panel at the end even before the result arrives', async () => {
        vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail({ endsAt: '2026-10-11T12:00:00Z' }))
        vi.mocked(fetchAuction).mockReturnValueOnce(new Promise(() => {}))
        await renderAt('/auctions/5', 3)
        await screen.findByRole('region', { name: 'Place a bid' })

        await vi.advanceTimersByTimeAsync(2_000)

        expect(screen.queryByRole('region', { name: 'Place a bid' })).toBeNull()
        expect(screen.getByText('Ended')).toBeTruthy()
      })

      it('keeps counting down when the auction was extended', async () => {
        vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail({ endsAt: '2026-10-11T12:00:00Z' }))
        vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail({ endsAt: '2026-10-11T12:00:30Z' }))
        await renderAt('/auctions/5', 3)
        await screen.findByRole('region', { name: 'Place a bid' })

        await vi.advanceTimersByTimeAsync(2_000)

        expect(screen.getByText('Ending')).toBeTruthy()
        expect(screen.getByRole('region', { name: 'Place a bid' })).toBeTruthy()
        expect(screen.queryByRole('region', { name: 'Result' })).toBeNull()
        await vi.advanceTimersByTimeAsync(10_000)
        expect(fetchAuction).toHaveBeenCalledTimes(2)
      })

      it('tries 3 times, 2 seconds apart, while the server still says open', async () => {
        vi.mocked(fetchAuction).mockResolvedValue(auctionDetail({ endsAt: '2026-10-11T12:00:00Z' }))
        await renderAt('/auctions/5', 3)
        await screen.findByRole('region', { name: 'Place a bid' })

        await vi.advanceTimersByTimeAsync(2_000)
        expect(fetchAuction).toHaveBeenCalledTimes(2)
        await vi.advanceTimersByTimeAsync(2_000)
        expect(fetchAuction).toHaveBeenCalledTimes(3)
        await vi.advanceTimersByTimeAsync(2_000)
        expect(fetchAuction).toHaveBeenCalledTimes(4)
        await vi.advanceTimersByTimeAsync(20_000)
        expect(fetchAuction).toHaveBeenCalledTimes(4)
        expect(screen.queryByRole('region', { name: 'Place a bid' })).toBeNull()
      })

      it('counts a failed fetch as one of the 3 tries', async () => {
        vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail({ endsAt: '2026-10-11T12:00:00Z' }))
        vi.mocked(fetchAuction).mockRejectedValueOnce(new ApiError(502, 'Bad gateway'))
        vi.mocked(fetchAuction).mockResolvedValueOnce(closed())
        await renderAt('/auctions/5', 3)
        await screen.findByRole('region', { name: 'Place a bid' })

        await vi.advanceTimersByTimeAsync(2_000)
        expect(fetchAuction).toHaveBeenCalledTimes(2)
        await vi.advanceTimersByTimeAsync(2_000)

        expect(await screen.findByText('Sold to B*** for USD 5,200.00')).toBeTruthy()
        expect(fetchAuction).toHaveBeenCalledTimes(3)
      })

      it('stops trying once the user leaves the page, even with a fetch on its way', async () => {
        let answer: (auction: ReturnType<typeof auctionDetail>) => void = () => {}
        vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail({ endsAt: '2026-10-11T12:00:00Z' }))
        vi.mocked(fetchAuction).mockReturnValueOnce(new Promise((resolve) => (answer = resolve)))
        vi.mocked(fetchAuction).mockResolvedValue(auctionDetail({ endsAt: '2026-10-11T12:00:00Z' }))
        await renderAt('/auctions/5', 3)
        await screen.findByRole('region', { name: 'Place a bid' })
        await vi.advanceTimersByTimeAsync(2_000)
        expect(fetchAuction).toHaveBeenCalledTimes(2)

        cleanup()
        answer(auctionDetail({ endsAt: '2026-10-11T12:00:00Z' }))
        await vi.advanceTimersByTimeAsync(20_000)

        expect(fetchAuction).toHaveBeenCalledTimes(2)
      })

      it('gives another auction with the same end time its own 3 tries', async () => {
        vi.mocked(fetchAuction).mockImplementation(async (id) =>
          auctionDetail({ id, endsAt: '2026-10-11T12:00:00Z' }),
        )
        const router = await renderAt('/auctions/5', 3)
        await screen.findByRole('region', { name: 'Price' })
        await vi.advanceTimersByTimeAsync(20_000)
        expect(fetchAuction).toHaveBeenCalledTimes(4)

        await router.push('/auctions/6')
        await vi.advanceTimersByTimeAsync(20_000)

        expect(vi.mocked(fetchAuction).mock.calls.filter(([id]) => id === 6)).toHaveLength(4)
      })
    })

    it('fetches the auction instead of patching it when a bid event arrives after it closed', async () => {
      vi.mocked(fetchAuction).mockResolvedValueOnce(closed({ currentBid: '5200.00', bidCount: 3, highestBidderId: 2 }))
      vi.mocked(fetchAuction).mockResolvedValueOnce(
        closed({ winnerName: 'C***', currentBid: '5300.00', bidCount: 4, highestBidderId: 3 }),
      )
      await renderAt('/auctions/5', 2)
      await screen.findByText('Sold to B*** for USD 5,200.00')

      FakeEventSource.latest().emit(
        bidPlaced({ bidderId: 3, bidderName: 'C***', highestBidderId: 3, amount: '5300.00', currentBid: '5300.00', bidCount: 4 }),
      )

      expect(await screen.findByText('Sold to C*** for USD 5,300.00')).toBeTruthy()
      expect(fetchAuction).toHaveBeenCalledTimes(2)
      expect(screen.queryByText(/You have been outbid/)).toBeNull()
    })
  })

  describe('outbid and live status', () => {
    it('tells me I was outbid and shows the new minimum when another user outbids me', async () => {
      vi.mocked(fetchAuction).mockResolvedValue(auctionDetail({ bidCount: 1, highestBidderId: 2 }))
      await renderAt('/auctions/5', 2)
      expect(await screen.findByText('You are the highest bidder.')).toBeTruthy()

      FakeEventSource.latest().emit(
        bidPlaced({ bidderId: 3, highestBidderId: 3, bidCount: 2, minNextBid: '12600.00' }),
      )

      expect(
        await screen.findByText('You have been outbid. The minimum bid is now USD 12,600.00.'),
      ).toBeTruthy()
      expect(screen.getByText('Minimum bid: USD 12,600.00')).toBeTruthy()
      expect(screen.queryByText('You are the highest bidder.')).toBeNull()
    })

    it('shows no outbid message for my own bid and says I am the highest bidder', async () => {
      vi.mocked(fetchAuction).mockResolvedValue(auctionDetail({ bidCount: 1, highestBidderId: 3 }))
      await renderAt('/auctions/5', 2)
      await screen.findByLabelText('Your bid (USD)')

      FakeEventSource.latest().emit(bidPlaced({ bidderId: 2, highestBidderId: 2, bidCount: 2 }))

      expect(await screen.findByText('You are the highest bidder.')).toBeTruthy()
      expect(screen.queryByText(/You have been outbid/)).toBeNull()
    })

    it('shows no outbid message when I was not the highest bidder', async () => {
      vi.mocked(fetchAuction).mockResolvedValue(auctionDetail({ bidCount: 1, highestBidderId: 3 }))
      await renderAt('/auctions/5', 2)
      await screen.findByLabelText('Your bid (USD)')

      FakeEventSource.latest().emit(
        bidPlaced({ bidderId: 4, highestBidderId: 4, bidCount: 2, minNextBid: '12600.00' }),
      )

      expect(await screen.findByText('Minimum bid: USD 12,600.00')).toBeTruthy()
      expect(screen.queryByText(/You have been outbid/)).toBeNull()
    })

    it('hides the outbid message when it is dismissed', async () => {
      vi.mocked(fetchAuction).mockResolvedValue(auctionDetail({ bidCount: 1, highestBidderId: 2 }))
      await renderAt('/auctions/5', 2)
      await screen.findByText('You are the highest bidder.')
      FakeEventSource.latest().emit(bidPlaced({ highestBidderId: 3, bidCount: 2 }))
      await screen.findByText(/You have been outbid/)

      await fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }))

      expect(screen.queryByText(/You have been outbid/)).toBeNull()
    })

    it('recalculates my status without reconnecting when I switch user', async () => {
      vi.mocked(fetchAuction).mockResolvedValue(auctionDetail({ bidCount: 1, highestBidderId: 2 }))
      await renderAt('/auctions/5', 3)
      await screen.findByLabelText('Your bid (USD)')

      useIdentityStore(pinia).setUser(2)

      expect(await screen.findByText('You are the highest bidder.')).toBeTruthy()
      expect(FakeEventSource.instances).toHaveLength(1)
    })

    it('clears the outbid message when I switch user', async () => {
      vi.mocked(fetchAuction).mockResolvedValue(auctionDetail({ bidCount: 1, highestBidderId: 2 }))
      await renderAt('/auctions/5', 2)
      await screen.findByText('You are the highest bidder.')
      FakeEventSource.latest().emit(bidPlaced({ highestBidderId: 3, bidCount: 2 }))
      await screen.findByText(/You have been outbid/)

      useIdentityStore(pinia).setUser(4)

      await vi.waitFor(() => expect(screen.queryByText(/You have been outbid/)).toBeNull())
    })

    it('shows my bid once when its event arrives after the page fetched it', async () => {
      const mine = { amount: '12000.00', placedAt: '2026-10-11T10:00:00Z', bidderName: 'B***' }
      vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail())
      vi.mocked(fetchAuction).mockResolvedValueOnce(
        auctionDetail({ bidCount: 1, highestBidderId: 2, minNextBid: '12100.00', bids: [mine] }),
      )
      vi.mocked(placeBid).mockResolvedValue({
        id: 42,
        auctionId: 5,
        bidderId: 2,
        amount: '12000.00',
        placedAt: '2026-10-11T10:00:00Z',
        _links: {},
      })
      await renderAt('/auctions/5', 2)
      await fireEvent.update(await screen.findByLabelText('Your bid (USD)'), '12000.00')
      await fireEvent.click(screen.getByRole('button', { name: 'Place bid' }))
      await screen.findByText('You are the highest bidder.')

      FakeEventSource.latest().emit(bidPlaced({ bidderId: 2, highestBidderId: 2, bidCount: 1 }))
      await flushPromises()

      const history = screen.getByRole('region', { name: 'Bid history' })
      expect(within(history).getAllByRole('listitem')).toHaveLength(1)
    })

    it('says nothing about live updates until the stream opens, then says live', async () => {
      vi.mocked(fetchAuction).mockResolvedValue(auctionDetail())
      await renderAt('/auctions/5')
      await screen.findByRole('region', { name: 'Price' })
      expect(screen.queryByText('Live')).toBeNull()

      FakeEventSource.latest().open()

      expect(await screen.findByText('Live')).toBeTruthy()
    })

    it('says it is reconnecting after an error', async () => {
      vi.mocked(fetchAuction).mockResolvedValue(auctionDetail())
      await renderAt('/auctions/5')
      await screen.findByRole('region', { name: 'Price' })
      FakeEventSource.latest().open()

      FakeEventSource.latest().fail()

      expect(await screen.findByText('Reconnecting')).toBeTruthy()
    })

    it('says live updates are unavailable after 5 errors and keeps the auction shown', async () => {
      vi.mocked(fetchAuction).mockResolvedValue(auctionDetail({ currentBid: '12345.00' }))
      await renderAt('/auctions/5')
      await screen.findByRole('region', { name: 'Price' })

      for (let i = 0; i < 5; i++) FakeEventSource.latest().fail()

      expect(await screen.findByText('Live updates unavailable')).toBeTruthy()
      expect(screen.getByText('USD 12,345.00')).toBeTruthy()
    })
  })
})
