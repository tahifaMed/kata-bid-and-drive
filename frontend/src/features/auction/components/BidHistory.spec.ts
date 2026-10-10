import { cleanup, render, screen, within } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'
import BidHistory from './BidHistory.vue'

afterEach(() => cleanup())

describe('BidHistory', () => {
  it('lists the bids in the order given, with amount and masked bidder name', () => {
    render(BidHistory, {
      props: {
        bids: [
          { amount: '12200.00', placedAt: '2026-10-11T10:02:00Z', bidderName: 'B***' },
          { amount: '12100.00', placedAt: '2026-10-11T10:01:00Z', bidderName: 'C***' },
        ],
      },
    })

    const rows = screen.getAllByRole('listitem')
    expect(rows).toHaveLength(2)
    expect(within(rows[0]!).getByText(/USD\s12,200\.00/)).toBeTruthy()
    expect(within(rows[0]!).getByText('B***')).toBeTruthy()
    expect(within(rows[1]!).getByText('C***')).toBeTruthy()
  })

  it('says there are no bids yet when the history is empty', () => {
    render(BidHistory, { props: { bids: [] } })

    expect(screen.getByText('No bids yet.')).toBeTruthy()
  })
})
