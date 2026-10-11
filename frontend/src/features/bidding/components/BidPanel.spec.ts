import { createPinia, setActivePinia } from 'pinia'
import { cleanup, fireEvent, render, screen } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useIdentityStore } from '../../identity/stores/identity'
import { auctionDetail } from '../../auction/testing/auctionDetailFixture'
import type { AuctionResponse } from '../../auction/types'
import BidPanel from './BidPanel.vue'

// The panel's real API call runs and fails or succeeds at the network boundary.
// A mocked API function that throws is reported by Vitest as a test error.
function answer(status: number, body: object) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status })))
}

const BID = {
  id: 12,
  auctionId: 5,
  bidderId: 2,
  amount: '12000.00',
  placedAt: '2026-10-11T10:00:00Z',
  _links: {
    self: { href: 'http://localhost/api/auctions/5/bids/12' },
    auction: { href: 'http://localhost/api/auctions/5' },
  },
}

beforeEach(() => {
  sessionStorage.clear()
  setActivePinia(createPinia())
  // The fixture's seller is user 1, so user 2 is a buyer.
  useIdentityStore().setUser(2)
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

function renderPanel(overrides: Partial<AuctionResponse> = {}) {
  return render(BidPanel, { props: { auction: auctionDetail(overrides) } })
}

function submitButton(): HTMLButtonElement {
  return screen.getByRole('button', { name: 'Place bid' })
}

async function bid(amount: string) {
  await fireEvent.update(screen.getByLabelText('Your bid (USD)'), amount)
  await fireEvent.click(submitButton())
}

describe('BidPanel', () => {
  it('shows the minimum bid and disables the button until the amount reaches it', async () => {
    renderPanel({ currentBid: '12000.00', minNextBid: '12100.00', bidCount: 1, highestBidderId: 3 })

    expect(screen.getByText('Minimum bid: USD 12,100.00')).toBeTruthy()
    expect(submitButton().disabled).toBe(true)

    await fireEvent.update(screen.getByLabelText('Your bid (USD)'), '12050.00')
    expect(submitButton().disabled).toBe(true)
    expect(screen.getByText('Your bid must be at least USD 12,100.00.')).toBeTruthy()

    await fireEvent.update(screen.getByLabelText('Your bid (USD)'), '12100.00')
    expect(submitButton().disabled).toBe(false)
  })

  it('disables the button for an amount that is not a valid price', async () => {
    renderPanel()

    await fireEvent.update(screen.getByLabelText('Your bid (USD)'), '12000.001')

    expect(submitButton().disabled).toBe(true)
    expect(
      screen.getByText('Enter an amount such as 12000.00, with at most two decimals.'),
    ).toBeTruthy()
  })

  it('posts the amount, clears the input, confirms and emits placed', async () => {
    answer(201, BID)
    const { emitted } = renderPanel()

    await bid('12000.00')

    expect(await screen.findByText('Your bid of USD 12,000.00 was placed.')).toBeTruthy()
    expect((screen.getByLabelText('Your bid (USD)') as HTMLInputElement).value).toBe('')
    expect(emitted('placed')).toHaveLength(1)
    const [url, init] = vi.mocked(fetch).mock.calls[0]!
    expect(url).toBe('/api/auctions/5/bids')
    expect(init?.method).toBe('POST')
    expect(init?.body).toBe('{"amount":"12000.00"}')
  })

  it('shows the minimum from a 422 and emits placed so the page refetches', async () => {
    answer(422, {
      title: 'Bid too low',
      detail: 'The bid must be at least 12100.00',
      minimumBid: '12100.00',
    })
    const { emitted } = renderPanel()

    await bid('12000.00')

    // Matched by text: the currency format puts a non-breaking space after "USD".
    expect(await screen.findByText('Your bid must be at least USD 12,100.00.')).toBe(
      screen.getByRole('alert'),
    )
    expect(emitted('placed')).toHaveLength(1)
  })

  it('shows the detail of a 409 and emits placed so the page refetches', async () => {
    answer(409, { title: 'Auction closed', detail: 'The auction is closed' })
    const { emitted } = renderPanel()

    await bid('12000.00')

    expect((await screen.findByRole('alert')).textContent).toContain('The auction is closed')
    expect(emitted('placed')).toHaveLength(1)
  })

  it('explains a version conflict in plain words', async () => {
    answer(409, { title: 'Bid conflict', detail: 'bid conflict, reload and retry' })
    renderPanel()

    await bid('12000.00')

    expect((await screen.findByRole('alert')).textContent).toContain(
      'Another bid was placed first. Check the new minimum and try again.',
    )
  })

  it('shows the field message of a 400 and keeps the amount', async () => {
    answer(400, {
      title: 'Invalid bid',
      errors: [{ field: 'amount', message: 'must be a positive amount with at most two decimals' }],
    })
    const { emitted } = renderPanel()

    await bid('12000.00')

    expect((await screen.findByRole('alert')).textContent).toContain(
      'Amount must be a positive amount with at most two decimals.',
    )
    expect((screen.getByLabelText('Your bid (USD)') as HTMLInputElement).value).toBe('12000.00')
    expect(emitted('placed')).toBeUndefined()
  })

  it.each([401, 403])('shows a generic message on %d and does not emit placed', async (status) => {
    answer(status, { title: 'Error' })
    const { emitted } = renderPanel()

    await bid('12000.00')

    expect((await screen.findByRole('alert')).textContent).toContain(
      'Could not place the bid. Try again.',
    )
    expect(emitted('placed')).toBeUndefined()
  })

  it('shows a generic message when the network fails', async () => {
    vi.stubGlobal('fetch', () => Promise.reject(new TypeError('Failed to fetch')))
    const { emitted } = renderPanel()

    await bid('12000.00')

    expect((await screen.findByRole('alert')).textContent).toContain(
      'Could not place the bid. Try again.',
    )
    expect(emitted('placed')).toBeUndefined()
    expect(submitButton().disabled).toBe(false)
  })

  it('tells the seller they cannot bid instead of showing the form', () => {
    useIdentityStore().setUser(1)
    renderPanel()

    expect(screen.getByText('You are the seller of this auction.')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Place bid' })).toBeNull()
  })

  it('tells the highest bidder they are winning instead of showing the form', () => {
    renderPanel({ currentBid: '12000.00', minNextBid: '12100.00', bidCount: 1, highestBidderId: 2 })

    expect(screen.getByText('You are the highest bidder.')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Place bid' })).toBeNull()
  })

  it('follows the user switcher without a reload', async () => {
    renderPanel({ currentBid: '12000.00', minNextBid: '12100.00', bidCount: 1, highestBidderId: 2 })
    expect(screen.getByText('You are the highest bidder.')).toBeTruthy()

    useIdentityStore().setUser(3)
    expect(await screen.findByRole('button', { name: 'Place bid' })).toBeTruthy()

    useIdentityStore().setUser(1)
    expect(await screen.findByText('You are the seller of this auction.')).toBeTruthy()
  })

  it('does not show a bid result to a user who switched in while it was on its way', async () => {
    let respond: (response: Response) => void = () => {}
    vi.stubGlobal('fetch', () => new Promise<Response>((resolve) => (respond = resolve)))
    const { emitted } = renderPanel()

    await bid('12000.00')
    useIdentityStore().setUser(3)
    respond(new Response(JSON.stringify(BID), { status: 201 }))

    await vi.waitFor(() => expect(emitted('placed')).toHaveLength(1))
    expect(screen.queryByText('Your bid of USD 12,000.00 was placed.')).toBeNull()
  })
})
