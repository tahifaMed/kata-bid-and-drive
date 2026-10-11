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

function renderHeader() {
  return render(AppHeader, { global: { plugins: [createPinia(), testRouter()] } })
}

describe('AppHeader', () => {
  it('links to the auctions and to my bids', () => {
    renderHeader()

    const nav = screen.getByRole('navigation', { name: 'Main' })
    expect(nav.querySelector('a[href="/"]')?.textContent).toBe('Auctions')
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

  it('links Sell a car to the create auction page on every page', () => {
    renderHeader()

    expect(screen.getByRole('link', { name: 'Sell a car' }).getAttribute('href')).toBe(
      '/auctions/new',
    )
  })

  it('shows only the pages that exist', () => {
    renderHeader()

    expect(screen.queryByText('Watch list')).toBeNull()
    expect(screen.queryByText('About')).toBeNull()
  })
})
