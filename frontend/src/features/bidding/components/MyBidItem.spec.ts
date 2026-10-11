import { cleanup, render, screen } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import MyBidItem from './MyBidItem.vue'
import type { MyBid } from '../types'

const NOW = Date.parse('2026-10-10T12:00:00Z')

function myBid(overrides: Partial<MyBid> = {}): MyBid {
  return {
    auctionId: 5,
    car: { make: 'Volkswagen', model: 'Golf', year: 2020, imageUrl: null },
    myHighestBid: '1200.00',
    currentBid: '1200.00',
    status: 'WINNING',
    endsAt: '2026-10-12T12:00:00Z',
    _links: { auction: { href: 'http://localhost/api/auctions/5' } },
    ...overrides,
  }
}

function renderItem(item: MyBid) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/auctions/:id', component: MyBidItem }],
  })
  render(MyBidItem, { props: { item, now: NOW }, global: { plugins: [router] } })
}

afterEach(() => cleanup())

describe('MyBidItem', () => {
  it('shows the car as the title and links to the auction', () => {
    renderItem(myBid())

    expect(screen.getByRole('heading', { name: '2020 Volkswagen Golf' })).toBeTruthy()
    expect(screen.getByRole('link').getAttribute('href')).toBe('/auctions/5')
  })

  it('shows a winning badge with my bid and the current bid', () => {
    renderItem(myBid())

    expect(screen.getByText('Winning')).toBeTruthy()
    expect(screen.getByText(/Your bid USD\s1,200\.00/)).toBeTruthy()
    expect(screen.getByText(/Current bid USD\s1,200\.00/)).toBeTruthy()
  })

  it('shows an outbid badge when someone bid higher', () => {
    renderItem(myBid({ status: 'OUTBID', myHighestBid: '900.00', currentBid: '1000.00' }))

    expect(screen.getByText('Outbid')).toBeTruthy()
    expect(screen.getByText(/Your bid USD\s900\.00/)).toBeTruthy()
    expect(screen.getByText(/Current bid USD\s1,000\.00/)).toBeTruthy()
  })

  it('shows a won badge with the winning style once I won', () => {
    renderItem(myBid({ status: 'WON', endsAt: '2026-10-10T12:00:00Z' }))

    const badge = screen.getByText('Won')
    expect(badge.classList.contains('status-winning')).toBe(true)
    expect(screen.getByText('Ended')).toBeTruthy()
  })

  it('shows a lost badge with the outbid style once I lost', () => {
    renderItem(myBid({ status: 'LOST', myHighestBid: '900.00', currentBid: '1000.00', endsAt: '2026-10-10T12:00:00Z' }))

    const badge = screen.getByText('Lost')
    expect(badge.classList.contains('status-outbid')).toBe(true)
    expect(screen.getByText('Ended')).toBeTruthy()
  })

  it('shows the time left while the auction runs', () => {
    renderItem(myBid())

    expect(screen.getByText('2d 0h')).toBeTruthy()
  })

  it('shows Ended once the end time has passed', () => {
    renderItem(myBid({ endsAt: '2026-10-10T12:00:00Z' }))

    expect(screen.getByText('Ended')).toBeTruthy()
  })
})
