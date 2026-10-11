import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch } from './client'
import { useIdentityStore } from '../features/identity/stores/identity'

beforeEach(() => {
  sessionStorage.clear()
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

  it('rejects with the status and the parsed ProblemDetail on an error status', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('{"title":"Bid too low","detail":"The bid must be at least 1100.00","minimumBid":"1100.00"}', {
          status: 422,
          headers: { 'Content-Type': 'application/problem+json' },
        }),
      ),
    )

    const error = await apiFetch('/api/auctions/5/bids').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(422)
    expect((error as ApiError).problem).toEqual({
      title: 'Bid too low',
      detail: 'The bid must be at least 1100.00',
      minimumBid: '1100.00',
    })
  })

  it('keeps the status without a problem when the error body is not JSON', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('<html>Bad gateway</html>', { status: 502 })))

    const error = await apiFetch('/api/auctions/5').catch((e: unknown) => e)

    expect((error as ApiError).status).toBe(502)
    expect((error as ApiError).problem).toBeUndefined()
  })
})
