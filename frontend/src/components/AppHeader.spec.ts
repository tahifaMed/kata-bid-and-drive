import { createPinia } from 'pinia'
import { cleanup, render, screen } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import AppHeader from './AppHeader.vue'
import * as usersApi from '../features/identity/api'

beforeEach(() => {
  sessionStorage.clear()
  vi.spyOn(usersApi, 'fetchUsers').mockResolvedValue([{ id: 1, displayName: 'Alice' }])
})

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

function testRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }],
  })
}

function renderHeader(slots: Record<string, string> = {}) {
  return render(AppHeader, { slots, global: { plugins: [createPinia(), testRouter()] } })
}

describe('AppHeader', () => {
  it('links to the auctions and to my bids', () => {
    renderHeader()

    const nav = screen.getByRole('navigation', { name: 'Main' })
    expect(nav.querySelector('a[href="/auctions"]')?.textContent).toBe('Auctions')
    expect(screen.getByRole('link', { name: 'My bids' }).getAttribute('href')).toBe('/me/bids')
  })

  it('links the brand to the home page', () => {
    renderHeader()

    expect(screen.getByRole('link', { name: 'BidAndDrive' }).getAttribute('href')).toBe('/')
  })

  it('shows the user switcher', async () => {
    renderHeader()

    expect(await screen.findByRole('combobox', { name: 'Acting as' })).toBeTruthy()
  })

  it('shows the actions a page adds', () => {
    renderHeader({ default: '<a href="/auctions/new">Sell a car</a>' })

    expect(screen.getByRole('link', { name: 'Sell a car' })).toBeTruthy()
  })
})
