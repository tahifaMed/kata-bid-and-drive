import { describe, expect, it } from 'vitest'
import { toCents } from './money'

describe('toCents', () => {
  it.each([
    ['1100', 110000],
    ['1100.5', 110050],
    ['1100.50', 110050],
    ['1100.500', 110050],
    ['0.01', 1],
    ['9999999999.99', 999999999999],
  ])('reads %s as %d cents', (amount, cents) => {
    expect(toCents(amount)).toBe(cents)
  })

  it.each(['', 'abc', ' 1100', '1100 ', '1e3', '-5', '0', '0.00', '1100.001', '10000000000.00', '1,100.00'])(
    'rejects %j, like the server',
    (amount) => {
      expect(toCents(amount)).toBeNull()
    },
  )
})
