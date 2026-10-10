import { cleanup, render, screen, within } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'
import AuctionsView from './AuctionsView.vue'
import { sampleAuctions } from '../data/sampleAuctions'

afterEach(() => cleanup())

describe('AuctionsView', () => {
  it('shows the live auctions heading', () => {
    render(AuctionsView)

    expect(screen.getByRole('heading', { level: 1, name: 'Live auctions' })).toBeTruthy()
  })

  it('lists exactly eight sample listings in the main content', () => {
    render(AuctionsView)

    const items = within(screen.getByRole('main')).getAllByRole('listitem')
    expect(items.length).toBe(8)
    expect(items.length).toBe(sampleAuctions.length)
  })

  it('credits every photo in the footer with its licence and a Commons link', () => {
    render(AuctionsView)

    const footer = screen.getByRole('contentinfo')
    for (const auction of sampleAuctions) {
      const link = within(footer).getByRole('link', { name: auction.title })
      expect(link.getAttribute('href')).toBe(auction.credit.url)
      expect(link.getAttribute('href')?.startsWith('https://commons.wikimedia.org/')).toBe(true)
      expect(footer.textContent).toContain(auction.credit.author)
      expect(footer.textContent).toContain(auction.credit.license)
    }
  })
})
