import { createPinia } from 'pinia'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchOpenAuctions } from '../api'
import AuctionsView from './AuctionsView.vue'
import type { AuctionListItem, AuctionListPage } from '../types'

vi.mock('../api', () => ({ fetchOpenAuctions: vi.fn<typeof fetchOpenAuctions>() }))

afterEach(() => cleanup())
beforeEach(() => {
  vi.mocked(fetchOpenAuctions).mockReset()
})

function testRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: AuctionsView },
      { path: '/auctions/new', component: AuctionsView },
      { path: '/auctions/:id', component: AuctionsView },
    ],
  })
}

function renderView() {
  return render(AuctionsView, { global: { plugins: [createPinia(), testRouter()] } })
}

function item(id: number, make: string, model: string): AuctionListItem {
  return {
    id,
    car: { make, model, year: 2020, imageUrl: null },
    currentBid: '12000.00',
    minNextBid: '12000.00',
    bidCount: 0,
    endsAt: '2026-10-12T12:00:00Z',
    _links: { self: { href: `http://localhost/api/auctions/${id}` } },
  }
}

function page(
  items: AuctionListItem[],
  { number = 0, next = false, prev = false } = {},
): AuctionListPage {
  const links: AuctionListPage['_links'] = {
    self: { href: `http://localhost/api/auctions?page=${number}&size=20` },
  }
  if (next) links.next = { href: `http://localhost/api/auctions?page=${number + 1}&size=20` }
  if (prev) links.prev = { href: `http://localhost/api/auctions?page=${number - 1}&size=20` }
  return { _embedded: { auctions: items }, page: { size: 20, number }, _links: links }
}

describe('AuctionsView', () => {
  it('shows the live auctions heading', () => {
    vi.mocked(fetchOpenAuctions).mockReturnValue(new Promise(() => {}))
    renderView()

    expect(screen.getByRole('heading', { level: 1, name: 'Live auctions' })).toBeTruthy()
  })

  it('shows a loading message while the auctions are on their way', () => {
    vi.mocked(fetchOpenAuctions).mockReturnValue(new Promise(() => {}))
    renderView()

    expect(screen.getByRole('status').textContent).toContain('Loading auctions')
  })

  it('lists the open auctions the API returns', async () => {
    vi.mocked(fetchOpenAuctions).mockResolvedValue(
      page([item(1, 'Porsche', '911'), item(2, 'Audi', 'S4')]),
    )
    renderView()

    const items = await within(await screen.findByRole('main')).findAllByRole('listitem')
    expect(items.length).toBe(2)
    expect(screen.getByRole('heading', { name: '2020 Porsche 911' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: '2020 Audi S4' })).toBeTruthy()
  })

  it('shows an empty message when no auction is open', async () => {
    vi.mocked(fetchOpenAuctions).mockResolvedValue(page([]))
    renderView()

    expect(await screen.findByText('No auctions are open right now.')).toBeTruthy()
  })

  it('shows an error with a retry that loads the page again', async () => {
    vi.mocked(fetchOpenAuctions)
      .mockRejectedValueOnce(new Error('network down'))
      .mockResolvedValueOnce(page([item(1, 'Porsche', '911')]))
    renderView()

    expect((await screen.findByRole('alert')).textContent).toContain('Could not load auctions.')
    await fireEvent.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('heading', { name: '2020 Porsche 911' })).toBeTruthy()
    expect(fetchOpenAuctions).toHaveBeenNthCalledWith(2, 0)
  })

  it('enables Next when the API gives a next link and loads the next page', async () => {
    vi.mocked(fetchOpenAuctions)
      .mockResolvedValueOnce(page([item(1, 'Porsche', '911')], { next: true }))
      .mockResolvedValueOnce(page([item(21, 'Audi', 'S4')], { number: 1, prev: true }))
    renderView()

    const next = await screen.findByRole('button', { name: 'Next' })
    expect(next.hasAttribute('disabled')).toBe(false)
    expect(screen.getByRole('button', { name: 'Previous' }).hasAttribute('disabled')).toBe(true)

    await fireEvent.click(next)

    expect(await screen.findByRole('heading', { name: '2020 Audi S4' })).toBeTruthy()
    expect(fetchOpenAuctions).toHaveBeenLastCalledWith(1)
    expect(screen.getByRole('button', { name: 'Previous' }).hasAttribute('disabled')).toBe(false)
  })

  it('after a failed Next, the page on screen stays put and Next again asks for the page after it', async () => {
    vi.mocked(fetchOpenAuctions)
      .mockResolvedValueOnce(page([item(1, 'Porsche', '911')], { next: true }))
      .mockRejectedValueOnce(new Error('network down'))
      .mockResolvedValueOnce(page([item(21, 'Audi', 'S4')], { number: 1, prev: true, next: true }))
    renderView()

    await fireEvent.click(await screen.findByRole('button', { name: 'Next' }))
    await screen.findByRole('alert')
    expect(screen.getByText('Page 1')).toBeTruthy()

    await fireEvent.click(screen.getByRole('button', { name: 'Next' }))

    expect(await screen.findByRole('heading', { name: '2020 Audi S4' })).toBeTruthy()
    expect(fetchOpenAuctions).toHaveBeenNthCalledWith(3, 1)
  })

  it('disables Next on the last page', async () => {
    vi.mocked(fetchOpenAuctions).mockResolvedValue(page([item(1, 'Porsche', '911')]))
    renderView()

    await screen.findByRole('heading', { name: '2020 Porsche 911' })
    expect(screen.getByRole('button', { name: 'Next' }).hasAttribute('disabled')).toBe(true)
  })

  it('links the sell button to the create auction page', () => {
    vi.mocked(fetchOpenAuctions).mockReturnValue(new Promise(() => {}))
    renderView()

    expect(screen.getByRole('link', { name: 'Sell a car' }).getAttribute('href')).toBe(
      '/auctions/new',
    )
  })

  it('shows no controls that are not wired up', () => {
    vi.mocked(fetchOpenAuctions).mockReturnValue(new Promise(() => {}))
    renderView()

    expect(screen.queryByRole('button', { name: 'Sign up' })).toBeNull()
    expect(screen.queryByRole('searchbox')).toBeNull()
    expect(screen.queryByRole('group', { name: 'Sort auctions' })).toBeNull()
    expect(screen.queryByRole('button', { name: /Years|Transmission|Body style/ })).toBeNull()
  })
})
