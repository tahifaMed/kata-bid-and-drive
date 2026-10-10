import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchOpenAuctions } from './api'

beforeEach(() => {
  setActivePinia(createPinia())
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{}', { status: 200 })))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('fetchOpenAuctions', () => {
  it('requests the page of open auctions from the list endpoint', async () => {
    await fetchOpenAuctions(2)

    expect(vi.mocked(fetch).mock.calls[0]?.[0]).toBe('/api/auctions?page=2&size=20')
  })
})
