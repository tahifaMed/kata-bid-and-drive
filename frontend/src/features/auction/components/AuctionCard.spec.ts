import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AuctionCard from './AuctionCard.vue'
import type { AuctionListing } from '../types'

const listing: AuctionListing = {
  id: 'test-car',
  title: '2017 Porsche 911 Carrera Coupe',
  specs: '15k miles, 7-speed manual, twin-turbo flat-six',
  location: 'Carmichael, CA',
  timeLeft: '2 days',
  currentBid: 81500,
  noReserve: false,
  image: '/images/auctions/test-car.jpg',
  imageAlt: 'Grey Porsche 911 Carrera coupe, three-quarter front view',
  credit: { author: 'Test Author', license: 'CC BY-SA 4.0', url: 'https://example.com' },
}

describe('AuctionCard', () => {
  it('shows the title, specs and location', () => {
    const wrapper = mount(AuctionCard, { props: { listing } })

    expect(wrapper.find('.auction-card-title').text()).toBe(listing.title)
    expect(wrapper.find('.auction-card-spec').text()).toBe(listing.specs)
    expect(wrapper.find('.auction-card-meta').text()).toBe(listing.location)
  })

  it('shows the time left and the current bid in US dollars', () => {
    const wrapper = mount(AuctionCard, { props: { listing } })
    const chip = wrapper.find('.image-chip').text()

    expect(chip).toContain('2 days')
    expect(chip).toContain('$81,500')
  })

  it('shows the no reserve badge only when the auction has no reserve', () => {
    expect(mount(AuctionCard, { props: { listing } }).find('.badge-info').exists()).toBe(false)

    const noReserve = { ...listing, noReserve: true }
    expect(
      mount(AuctionCard, { props: { listing: noReserve } })
        .find('.badge-info')
        .text(),
    ).toBe('No reserve')
  })

  it('uses the photo with its alt text', () => {
    const img = mount(AuctionCard, { props: { listing } }).find('img')

    expect(img.attributes('src')).toBe(listing.image)
    expect(img.attributes('alt')).toBe(listing.imageAlt)
  })
})
