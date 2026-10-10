import { cleanup, render, screen } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'
import AuctionCard from './AuctionCard.vue'
import type { AuctionListing } from '../types'

const listing: AuctionListing = {
  id: 'test-car',
  title: '2013 Porsche 911 Carrera 4S',
  specs: '15k miles, 7-speed manual, naturally aspirated flat-six',
  location: 'Carmichael, CA',
  timeLeft: '2 days',
  currentBid: 81500,
  noReserve: false,
  image: '/images/auctions/test-car.jpg',
  imageAlt: '2013 Porsche 911 Carrera 4S coupe, front three-quarter view',
  credit: { author: 'Test Author', license: 'CC BY-SA 4.0', url: 'https://example.com' },
}

afterEach(() => cleanup())

describe('AuctionCard', () => {
  it('shows the title, specs and location', () => {
    render(AuctionCard, { props: { listing } })

    // getBy* throws when the element is missing, so these also assert presence
    expect(screen.getByRole('heading', { name: listing.title })).toBeTruthy()
    expect(screen.getByText(listing.specs)).toBeTruthy()
    expect(screen.getByText(listing.location)).toBeTruthy()
  })

  it('shows the time left and the current bid with a currency code and two decimals', () => {
    render(AuctionCard, { props: { listing } })

    expect(screen.getByText('2 days')).toBeTruthy()
    expect(screen.getByText(/USD\s81,500\.00/)).toBeTruthy()
  })

  it('shows the no reserve badge only when the auction has no reserve', () => {
    const { unmount } = render(AuctionCard, { props: { listing } })
    expect(screen.queryByText('No reserve')).toBeNull()
    unmount()

    render(AuctionCard, { props: { listing: { ...listing, noReserve: true } } })
    expect(screen.getByText('No reserve')).toBeTruthy()
  })

  it('shows the photo with its alt text', () => {
    render(AuctionCard, { props: { listing } })

    const img = screen.getByRole('img', { name: listing.imageAlt })
    expect(img.getAttribute('src')).toBe(listing.image)
  })
})
