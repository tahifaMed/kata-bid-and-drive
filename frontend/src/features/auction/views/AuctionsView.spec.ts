import { createPinia } from 'pinia'
import { cleanup, render, screen, within } from '@testing-library/vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it } from 'vitest'
import AuctionsView from './AuctionsView.vue'
import { sampleAuctions } from '../data/sampleAuctions'

afterEach(() => cleanup())

function testRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/', component: AuctionsView }, { path: '/auctions/new', component: AuctionsView }],
  })
}

describe('AuctionsView', () => {
  it('shows the live auctions heading', () => {
    render(AuctionsView, { global: { plugins: [createPinia(), testRouter()] } })

    expect(screen.getByRole('heading', { level: 1, name: 'Live auctions' })).toBeTruthy()
  })

  it('lists exactly eight sample listings in the main content', () => {
    render(AuctionsView, { global: { plugins: [createPinia(), testRouter()] } })

    const items = within(screen.getByRole('main')).getAllByRole('listitem')
    expect(items.length).toBe(8)
    expect(items.length).toBe(sampleAuctions.length)
  })

  it('links the sell button to the create auction page', () => {
    render(AuctionsView, { global: { plugins: [createPinia(), testRouter()] } })

    expect(screen.getByRole('link', { name: 'Sell a car' }).getAttribute('href')).toBe('/auctions/new')
  })

  it('credits every photo in the footer with its licence and a Commons link', () => {
    render(AuctionsView, { global: { plugins: [createPinia(), testRouter()] } })

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
