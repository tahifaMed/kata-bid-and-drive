import { effectScope } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useNow } from './useNow'

beforeEach(() => {
  vi.useFakeTimers()
  vi.setSystemTime(new Date('2026-10-11T12:00:00Z'))
})
afterEach(() => vi.useRealTimers())

describe('useNow', () => {
  it('starts at the current time and moves on once per second', () => {
    const scope = effectScope()
    const now = scope.run(() => useNow())!
    expect(now.value).toBe(Date.parse('2026-10-11T12:00:00Z'))

    vi.advanceTimersByTime(1000)

    expect(now.value).toBe(Date.parse('2026-10-11T12:00:01Z'))
    scope.stop()
  })

  it('stops ticking when its scope is disposed', () => {
    const scope = effectScope()
    const now = scope.run(() => useNow())!

    scope.stop()
    vi.advanceTimersByTime(5000)

    expect(now.value).toBe(Date.parse('2026-10-11T12:00:00Z'))
  })
})
