import { cleanup, render, screen } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'
import { auctionDetail } from '../testing/auctionDetailFixture'
import PriceSummary from './PriceSummary.vue'

afterEach(() => cleanup())

const ENDS_AT = '2026-10-12T18:00:00Z'

describe('PriceSummary', () => {
  it('shows the current bid, the minimum next bid and the bid count', () => {
    const auction = auctionDetail({ currentBid: '12600.00', minNextBid: '12700.00', bidCount: 3 })
    render(PriceSummary, { props: { auction, now: Date.parse(ENDS_AT) - 60 * 60_000 } })

    expect(screen.getByText(/USD\s12,600\.00/)).toBeTruthy()
    expect(screen.getByText(/USD\s12,700\.00/)).toBeTruthy()
    expect(screen.getByText('3')).toBeTruthy()
  })

  it('shows the time left before the end', () => {
    render(PriceSummary, {
      props: {
        auction: auctionDetail({ endsAt: ENDS_AT }),
        now: Date.parse(ENDS_AT) - (3 * 60 + 12) * 60_000,
      },
    })

    expect(screen.getByText('3h 12m')).toBeTruthy()
  })

  it('shows the final price and no minimum once a closed auction is sold', () => {
    const auction = auctionDetail({
      status: 'CLOSED',
      outcome: 'SOLD',
      winnerName: 'B***',
      currentBid: '12600.00',
      minNextBid: '12700.00',
      bidCount: 3,
      endsAt: ENDS_AT,
    })
    render(PriceSummary, { props: { auction, now: Date.parse(ENDS_AT) } })

    expect(screen.getByText('Final price')).toBeTruthy()
    expect(screen.getByText(/USD\s12,600\.00/)).toBeTruthy()
    expect(screen.queryByText('Minimum next bid')).toBeNull()
    expect(screen.queryByText(/USD\s12,700\.00/)).toBeNull()
    expect(screen.queryByText('Current bid')).toBeNull()
  })

  it('keeps the starting price label when a closed auction had no bids', () => {
    const auction = auctionDetail({ status: 'CLOSED', outcome: 'NO_BIDS', endsAt: ENDS_AT })
    render(PriceSummary, { props: { auction, now: Date.parse(ENDS_AT) } })

    expect(screen.getByText('Starting price')).toBeTruthy()
    expect(screen.queryByText('Final price')).toBeNull()
    expect(screen.queryByText('Minimum next bid')).toBeNull()
  })

  it('says ended once the end time has passed', () => {
    render(PriceSummary, {
      props: { auction: auctionDetail({ endsAt: ENDS_AT }), now: Date.parse(ENDS_AT) },
    })

    expect(screen.getByText('Ended')).toBeTruthy()
  })
})
