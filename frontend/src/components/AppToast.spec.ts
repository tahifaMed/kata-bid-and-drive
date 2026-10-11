import { cleanup, fireEvent, render, screen } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AppToast from './AppToast.vue'

beforeEach(() => vi.useFakeTimers())
afterEach(() => {
  cleanup()
  vi.useRealTimers()
})

describe('AppToast', () => {
  it('announces its message politely', () => {
    render(AppToast, { props: { message: 'You have been outbid.' } })

    expect(screen.getByRole('status').textContent).toContain('You have been outbid.')
  })

  it('asks to be dismissed when the dismiss button is pressed', async () => {
    const { emitted } = render(AppToast, { props: { message: 'You have been outbid.' } })

    await fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }))

    expect(emitted('dismiss')).toHaveLength(1)
  })

  it('asks to be dismissed after 6 seconds', () => {
    const { emitted } = render(AppToast, { props: { message: 'You have been outbid.' } })

    vi.advanceTimersByTime(5999)
    expect(emitted('dismiss')).toBeUndefined()
    vi.advanceTimersByTime(1)

    expect(emitted('dismiss')).toHaveLength(1)
  })

  it('gives a new message its own 6 seconds', async () => {
    const { emitted, rerender } = render(AppToast, { props: { message: 'First' } })
    vi.advanceTimersByTime(4000)

    await rerender({ message: 'Second' })
    vi.advanceTimersByTime(4000)

    expect(emitted('dismiss')).toBeUndefined()
  })
})
