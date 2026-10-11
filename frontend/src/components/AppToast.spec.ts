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

  it('keeps an empty status region while there is no message, so a new one is announced', () => {
    render(AppToast, { props: { message: null } })

    expect(screen.getByRole('status').textContent?.trim()).toBe('')
    expect(screen.queryByRole('button', { name: 'Dismiss' })).toBeNull()
  })

  it('shows a message given to the region that was already there', async () => {
    const { rerender } = render(AppToast, { props: { message: null } })

    await rerender({ message: 'You have been outbid.' })

    expect(screen.getByRole('status').textContent).toContain('You have been outbid.')
  })

  it('does not ask to be dismissed while there is no message', () => {
    const { emitted } = render(AppToast, { props: { message: null } })

    vi.advanceTimersByTime(6000)

    expect(emitted('dismiss')).toBeUndefined()
  })
})
