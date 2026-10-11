import { createPinia, setActivePinia } from 'pinia'
import { cleanup, fireEvent, render, screen } from '@testing-library/vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { ApiError } from '../../../api/client'
import { useIdentityStore } from '../../identity/stores/identity'
import * as usersApi from '../../identity/api'
import { fetchMyBids } from '../api'
import type { MyBid, MyBidsCollection } from '../types'
import MyBidsView from './MyBidsView.vue'

vi.mock('../api', () => ({ fetchMyBids: vi.fn<typeof fetchMyBids>() }))

function myBid(auctionId: number, model: string, status: MyBid['status']): MyBid {
  return {
    auctionId,
    car: { make: 'Volkswagen', model, year: 2020, imageUrl: null },
    myHighestBid: '1200.00',
    currentBid: '1300.00',
    status,
    endsAt: '2100-01-01T00:00:00Z',
    _links: { auction: { href: `http://localhost/api/auctions/${auctionId}` } },
  }
}

function collection(...items: MyBid[]): MyBidsCollection {
  return { _embedded: { myBids: items }, _links: { self: { href: 'http://localhost/api/me/bids' } } }
}

beforeEach(() => {
  sessionStorage.clear()
  vi.mocked(fetchMyBids).mockReset()
  vi.spyOn(usersApi, 'fetchUsers').mockResolvedValue([
    { id: 2, displayName: 'Bob' },
    { id: 3, displayName: 'Chloe' },
  ])
})

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

function renderView(userId = 2) {
  sessionStorage.setItem('bidanddrive.currentUserId', String(userId))
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:pathMatch(.*)*', component: { render: () => null } }],
  })
  render(MyBidsView, { global: { plugins: [pinia, router] } })
  return useIdentityStore()
}

describe('MyBidsView', () => {
  it('shows a loading message while the bids are on their way', () => {
    vi.mocked(fetchMyBids).mockReturnValue(new Promise(() => {}))
    renderView()

    expect(screen.getByText('Loading your bids…')).toBeTruthy()
  })

  it('lists each auction I bid on with its status', async () => {
    vi.mocked(fetchMyBids).mockResolvedValue(
      collection(myBid(6, 'Polo', 'OUTBID'), myBid(5, 'Golf', 'WINNING')),
    )
    renderView()

    const headings = await screen.findAllByRole('heading', { level: 2 })
    expect(headings.map((heading) => heading.textContent)).toEqual([
      '2020 Volkswagen Polo',
      '2020 Volkswagen Golf',
    ])
    expect(screen.getByText('Outbid')).toBeTruthy()
    expect(screen.getByText('Winning')).toBeTruthy()
  })

  it('says so when I have not placed any bids', async () => {
    vi.mocked(fetchMyBids).mockResolvedValue(collection())
    renderView()

    expect(await screen.findByText('You have not placed any bids yet.')).toBeTruthy()
  })

  it('shows an error with a retry that asks again', async () => {
    vi.mocked(fetchMyBids)
      .mockRejectedValueOnce(new ApiError(401, 'Unauthorized'))
      .mockResolvedValueOnce(collection(myBid(5, 'Golf', 'WINNING')))
    renderView()

    expect(await screen.findByText('Could not load your bids.')).toBeTruthy()
    await fireEvent.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByText('Winning')).toBeTruthy()
    expect(fetchMyBids).toHaveBeenCalledTimes(2)
  })

  it('reloads the list for the newly selected user', async () => {
    vi.mocked(fetchMyBids)
      .mockResolvedValueOnce(collection(myBid(5, 'Golf', 'WINNING')))
      .mockResolvedValueOnce(collection())
    const identity = renderView()
    await screen.findByText('Winning')

    identity.setUser(3)

    expect(await screen.findByText('You have not placed any bids yet.')).toBeTruthy()
    expect(fetchMyBids).toHaveBeenCalledTimes(2)
  })

  it('drops a late answer for the previous user', async () => {
    let answerForBob: (value: MyBidsCollection) => void = () => {}
    vi.mocked(fetchMyBids)
      .mockReturnValueOnce(new Promise((resolve) => (answerForBob = resolve)))
      .mockResolvedValueOnce(collection())
    const identity = renderView()

    identity.setUser(3)
    expect(await screen.findByText('You have not placed any bids yet.')).toBeTruthy()
    answerForBob(collection(myBid(5, 'Golf', 'WINNING')))
    await nextTick()
    await nextTick()

    expect(screen.queryByText('Winning')).toBeNull()
    expect(screen.getByText('You have not placed any bids yet.')).toBeTruthy()
  })
})
