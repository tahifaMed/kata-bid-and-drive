import { createPinia } from 'pinia'
import { cleanup, fireEvent, render, screen } from '@testing-library/vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../api/client'
import { fetchAuction } from '../api'
import { auctionDetail } from '../testing/auctionDetailFixture'
import AuctionDetailView from './AuctionDetailView.vue'

vi.mock('../api', () => ({ fetchAuction: vi.fn<typeof fetchAuction>() }))

afterEach(() => cleanup())
beforeEach(() => {
  vi.mocked(fetchAuction).mockReset()
})

async function renderAt(path: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/auctions', component: { render: () => null } },
      { path: '/auctions/:id', component: AuctionDetailView },
    ],
  })
  await router.push(path)
  await router.isReady()
  render(AuctionDetailView, { global: { plugins: [createPinia(), router] } })
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
      '/auctions',
    )
  })

  it('shows the bid panel slot while the auction takes bids', async () => {
    vi.mocked(fetchAuction).mockResolvedValue(auctionDetail())
    await renderAt('/auctions/5')

    expect(await screen.findByRole('region', { name: 'Place a bid' })).toBeTruthy()
  })

  it('hides the bid panel slot when the auction has no place-bid link', async () => {
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
})
