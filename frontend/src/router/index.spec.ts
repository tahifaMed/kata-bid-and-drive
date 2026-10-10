import { describe, expect, it } from 'vitest'
import router from './index'

describe('router', () => {
  it('opens the auction detail for an auction path, as the list cards link to it', () => {
    expect(router.resolve('/auctions/5').name).toBe('auction-detail')
    expect(router.resolve('/auctions/5').params.id).toBe('5')
  })

  it('keeps the sell page on its own path', () => {
    expect(router.resolve('/auctions/new').name).toBe('auctions-new')
  })
})
