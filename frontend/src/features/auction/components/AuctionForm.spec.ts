import { cleanup, fireEvent, render, screen } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createAuction } from '../api'
import AuctionForm from './AuctionForm.vue'
import { fillValidAuctionForm } from '../testing/auctionFormFixture'
import type { AuctionResponse } from '../types'

vi.mock('../api', () => ({ createAuction: vi.fn<typeof createAuction>() }))

const createdAuction = { id: 5 } as AuctionResponse

afterEach(() => cleanup())
beforeEach(() => vi.mocked(createAuction).mockReset())

describe('AuctionForm', () => {
  it('keeps the submit button disabled while the form is incomplete', () => {
    render(AuctionForm)

    expect(screen.getByRole('button', { name: 'Create auction' }).hasAttribute('disabled')).toBe(true)
  })

  it('shows the make error once the make field is left empty', async () => {
    render(AuctionForm)

    await fireEvent.blur(screen.getByLabelText('Car make'))

    expect(screen.getByText('Enter a value.')).toBeTruthy()
    expect(screen.getByLabelText('Car make').getAttribute('aria-invalid')).toBe('true')
  })

  it('sends the auction once when the form is valid and reports the created auction', async () => {
    vi.mocked(createAuction).mockResolvedValue(createdAuction)
    const { emitted } = render(AuctionForm)

    await fillValidAuctionForm()
    await fireEvent.click(screen.getByRole('button', { name: 'Create auction' }))

    expect(createAuction).toHaveBeenCalledTimes(1)
    expect(vi.mocked(createAuction).mock.calls[0]?.[0].startingPrice).toBe('12000.00')
    expect(emitted().created).toEqual([[createdAuction]])
  })

  it('does not send a second request while the first is pending', async () => {
    let finishRequest: (auction: AuctionResponse) => void = () => {}
    vi.mocked(createAuction).mockReturnValue(
      new Promise<AuctionResponse>((resolve) => {
        finishRequest = resolve
      }),
    )
    const { container } = render(AuctionForm)

    await fillValidAuctionForm()
    await fireEvent.click(screen.getByRole('button', { name: 'Create auction' }))
    await fireEvent.submit(container.querySelector('form') as HTMLFormElement)

    expect(createAuction).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: 'Creating auction…' }).hasAttribute('disabled')).toBe(true)
    finishRequest(createdAuction)
  })
})
