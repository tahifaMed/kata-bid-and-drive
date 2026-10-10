import { describe, expect, it } from 'vitest'
import { formatCurrency, formatTimeLeft } from './format'

const NOW = Date.parse('2026-10-10T12:00:00Z')
const minutesFromNow = (minutes: number) => new Date(NOW + minutes * 60_000).toISOString()

describe('formatCurrency', () => {
  it('shows a string amount with a currency code and two decimals', () => {
    expect(formatCurrency('12600.00')).toMatch(/USD\s12,600\.00/)
  })

  it('keeps the cents of a string amount', () => {
    expect(formatCurrency('99.50')).toMatch(/USD\s99\.50/)
  })
})

describe('formatTimeLeft', () => {
  it('shows days and hours when a day or more is left', () => {
    expect(formatTimeLeft(minutesFromNow(2 * 24 * 60 + 4 * 60), NOW)).toBe('2d 4h')
  })

  it('shows hours and minutes when less than a day is left', () => {
    expect(formatTimeLeft(minutesFromNow(3 * 60 + 12), NOW)).toBe('3h 12m')
  })

  it('shows minutes when less than an hour is left', () => {
    expect(formatTimeLeft(minutesFromNow(12), NOW)).toBe('12m')
  })

  it('says ending when under a minute is left', () => {
    expect(formatTimeLeft(new Date(NOW + 30_000).toISOString(), NOW)).toBe('Ending')
  })

  it('says ending when the end time has passed', () => {
    expect(formatTimeLeft(minutesFromNow(-5), NOW)).toBe('Ending')
  })
})
