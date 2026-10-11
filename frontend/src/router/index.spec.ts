import { describe, expect, it } from 'vitest'
import router from './index'

describe('router', () => {
  it('opens the auctions list at the root', () => {
    expect(router.resolve('/').name).toBe('auctions')
  })

  it('redirects the old auctions path to the root', () => {
    expect(router.resolve('/auctions').matched[0]?.redirect).toBe('/')
  })

  it('opens the auction detail for an auction path, as the list cards link to it', () => {
    expect(router.resolve('/auctions/5').name).toBe('auction-detail')
    expect(router.resolve('/auctions/5').params.id).toBe('5')
  })

  it('opens my bids from its own path, as the header links to it', () => {
    expect(router.resolve('/me/bids').name).toBe('my-bids')
  })

  it('keeps the sell page on its own path', () => {
    expect(router.resolve('/auctions/new').name).toBe('auctions-new')
  })
})
