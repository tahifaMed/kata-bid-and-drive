import { cleanup, render, screen } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import AuctionCard from './AuctionCard.vue'
import type { AuctionListItem } from '../types'

const endsAt = '2026-10-12T12:00:00Z'
const NOW = Date.parse('2026-10-10T12:00:00Z')

const auction: AuctionListItem = {
  id: 7,
  car: { make: 'Porsche', model: '911 Carrera 4S', year: 2013, imageUrl: 'https://example.com/911.jpg' },
  currentBid: '81500.00',
  minNextBid: '81500.00',
  bidCount: 0,
  endsAt,
  _links: { self: { href: 'http://localhost/api/auctions/7' } },
}

function testRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/auctions/:id', component: AuctionCard }],
  })
}

afterEach(() => cleanup())

describe('AuctionCard', () => {
  it('shows the year, make and model as the title', () => {
    render(AuctionCard, { props: { auction, now: NOW }, global: { plugins: [testRouter()] } })

    expect(screen.getByRole('heading', { name: '2013 Porsche 911 Carrera 4S' })).toBeTruthy()
  })

  it('shows the time left and the current bid with a currency code and two decimals', () => {
    render(AuctionCard, { props: { auction, now: NOW }, global: { plugins: [testRouter()] } })

    expect(screen.getByText('2d 0h')).toBeTruthy()
    expect(screen.getByText(/USD\s81,500\.00/)).toBeTruthy()
  })

  it('shows the photo with the title as its alt text', () => {
    render(AuctionCard, { props: { auction, now: NOW }, global: { plugins: [testRouter()] } })

    const img = screen.getByRole('img', { name: '2013 Porsche 911 Carrera 4S' })
    expect(img.getAttribute('src')).toBe('https://example.com/911.jpg')
  })

  it('shows the neutral placeholder when the auction has no photo', () => {
    const { container } = render(AuctionCard, {
      props: { auction: { ...auction, car: { ...auction.car, imageUrl: null } }, now: NOW },
      global: { plugins: [testRouter()] },
    })

    expect(container.querySelector('img')?.getAttribute('src')).toBe('/images/auctions/no-photo.svg')
  })

  it('links the whole card to the auction page', () => {
    render(AuctionCard, { props: { auction, now: NOW }, global: { plugins: [testRouter()] } })

    expect(screen.getByRole('link', { name: /2013 Porsche 911 Carrera 4S/ }).getAttribute('href')).toBe('/auctions/7')
  })
})
