import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useIdentityStore } from './identity'
import * as usersApi from '../api'

const STORAGE_KEY = 'bidanddrive.currentUserId'

const seededUsers = [
  { id: 1, displayName: 'Alice' },
  { id: 2, displayName: 'Bob' },
  { id: 3, displayName: 'Chloe' },
  { id: 4, displayName: 'Dan' },
]

beforeEach(() => {
  sessionStorage.clear()
  localStorage.clear()
  setActivePinia(createPinia())
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('identity store', () => {
  it('selects user 1 when no choice has been stored', () => {
    expect(useIdentityStore().currentUserId).toBe(1)
  })

  it('ignores a choice made in another tab', () => {
    localStorage.setItem(STORAGE_KEY, '2')

    expect(useIdentityStore().currentUserId).toBe(1)
  })

  it('does not share the chosen user with other tabs', () => {
    useIdentityStore().setUser(2)

    expect(localStorage.getItem(STORAGE_KEY)).toBeNull()
  })

  it('keeps the chosen user after a page reload', () => {
    useIdentityStore().setUser(2)

    setActivePinia(createPinia())

    expect(useIdentityStore().currentUserId).toBe(2)
  })

  it('resets a stored user that is no longer seeded to user 1 once users load', async () => {
    sessionStorage.setItem(STORAGE_KEY, '9')
    vi.spyOn(usersApi, 'fetchUsers').mockResolvedValue(seededUsers)

    const store = useIdentityStore()
    await store.loadUsers()

    expect(store.currentUserId).toBe(1)
    expect(sessionStorage.getItem(STORAGE_KEY)).toBe('1')
  })

  it('falls back to user 1 when storage cannot be read', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('storage blocked')
    })

    expect(useIdentityStore().currentUserId).toBe(1)
  })

  it('keeps the choice in memory when storage cannot be written', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('storage blocked')
    })
    const store = useIdentityStore()

    expect(() => store.setUser(3)).not.toThrow()
    expect(store.currentUserId).toBe(3)
  })
})
