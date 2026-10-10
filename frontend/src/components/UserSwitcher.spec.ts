import { createPinia } from 'pinia'
import { cleanup, fireEvent, render, screen } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import UserSwitcher from './UserSwitcher.vue'
import * as usersApi from '../features/identity/api'

const seededUsers = [
  { id: 1, displayName: 'Alice' },
  { id: 2, displayName: 'Bob' },
  { id: 3, displayName: 'Chloe' },
  { id: 4, displayName: 'Dan' },
]

beforeEach(() => {
  localStorage.clear()
  vi.spyOn(usersApi, 'fetchUsers').mockResolvedValue(seededUsers)
})

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

function renderSwitcher() {
  return render(UserSwitcher, { global: { plugins: [createPinia()] } })
}

describe('UserSwitcher', () => {
  it('offers every seeded user by display name', async () => {
    renderSwitcher()

    expect(await screen.findByRole('option', { name: 'Alice' })).toBeTruthy()
    for (const user of seededUsers) {
      expect(screen.getByRole('option', { name: user.displayName })).toBeTruthy()
    }
  })

  it('selects Alice by default', async () => {
    renderSwitcher()
    await screen.findByRole('option', { name: 'Bob' })

    expect((screen.getByRole('combobox', { name: 'Acting as' }) as HTMLSelectElement).value).toBe('1')
  })

  it('shows Bob as the acting user once Bob is picked', async () => {
    renderSwitcher()
    await screen.findByRole('option', { name: 'Bob' })

    await fireEvent.update(screen.getByRole('combobox', { name: 'Acting as' }), '2')

    expect((screen.getByRole('combobox', { name: 'Acting as' }) as HTMLSelectElement).value).toBe('2')
  })
})
