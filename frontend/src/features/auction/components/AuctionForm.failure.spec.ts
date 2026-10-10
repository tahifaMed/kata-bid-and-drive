import { createPinia, setActivePinia } from 'pinia'
import { cleanup, fireEvent, render, screen } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AuctionForm from './AuctionForm.vue'
import { fillValidAuctionForm } from '../testing/auctionFormFixture'

// Fails at the network boundary, so the real API function and its error path run.
// A mocked API function that throws is reported by Vitest as a test error.
beforeEach(() => {
  setActivePinia(createPinia())
  vi.stubGlobal('fetch', () => Promise.reject(new TypeError('Failed to fetch')))
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('AuctionForm when the request fails', () => {
  it('shows a retry message and keeps the entered values', async () => {
    render(AuctionForm)

    await fillValidAuctionForm()
    await fireEvent.click(screen.getByRole('button', { name: 'Create auction' }))

    expect(await screen.findByText('Could not create the auction. Try again.')).toBeTruthy()
    expect((screen.getByLabelText('Car make') as HTMLInputElement).value).toBe('Volkswagen')
    expect(screen.getByRole('button', { name: 'Create auction' }).hasAttribute('disabled')).toBe(false)
  })

  it('shows the same retry message when the server answers with an error status', async () => {
    vi.stubGlobal('fetch', () => Promise.resolve(new Response('{"title":"Invalid auction"}', { status: 400 })))
    render(AuctionForm)

    await fillValidAuctionForm()
    await fireEvent.click(screen.getByRole('button', { name: 'Create auction' }))

    expect(await screen.findByText('Could not create the auction. Try again.')).toBeTruthy()
  })
})
