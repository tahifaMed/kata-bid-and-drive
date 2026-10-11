import { createPinia } from 'pinia'
import { cleanup, fireEvent, render, screen } from '@testing-library/vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createAuction } from '../api'
import CreateAuctionView from './CreateAuctionView.vue'
import { fillValidAuctionForm } from '../testing/auctionFormFixture'

vi.mock('../api', () => ({ createAuction: vi.fn<typeof createAuction>() }))

function testRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/auctions/new', component: CreateAuctionView },
      { path: '/', component: { template: '<div />' } },
      { path: '/auctions/:id', component: { template: '<div />' } },
    ],
  })
}

afterEach(() => cleanup())

describe('CreateAuctionView', () => {
  it('shows a confirmation with a link to the new auction after it is created', async () => {
    const router = testRouter()
    await router.push('/auctions/new')
    await router.isReady()
    vi.mocked(createAuction).mockResolvedValue({
      id: 5,
      car: { make: 'Volkswagen', model: 'Golf', year: 2020, mileageKm: 42000, description: null, imageUrl: null },
    } as never)
    render(CreateAuctionView, {
      global: { plugins: [createPinia(), router], stubs: { UserSwitcher: true } },
    })

    await fillValidAuctionForm()
    await fireEvent.click(screen.getByRole('button', { name: 'Create auction' }))

    expect(await screen.findByRole('heading', { name: 'Your auction is open' })).toBeTruthy()
    expect(screen.getByRole('link', { name: 'View your auction' }).getAttribute('href')).toBe('/auctions/5')
  })
})
