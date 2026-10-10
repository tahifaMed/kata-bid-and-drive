import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch } from './client'
import { useIdentityStore } from '../features/identity/stores/identity'

beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('[]', { status: 200 })))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function sentUserHeader(): string | null {
  const call = vi.mocked(fetch).mock.calls[0]
  return new Headers(call?.[1]?.headers).get('X-User-Id')
}

describe('apiFetch', () => {
  it('sends the header for the user selected in the identity store', async () => {
    useIdentityStore().setUser(2)

    await apiFetch('/api/users')

    expect(sentUserHeader()).toBe('2')
  })

  it('sends user 1 when nothing has been chosen', async () => {
    await apiFetch('/api/users')

    expect(sentUserHeader()).toBe('1')
  })
})
