import { cleanup, render, screen } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'
import { auctionDetail } from '../testing/auctionDetailFixture'
import AuctionResultBanner from './AuctionResultBanner.vue'

afterEach(() => cleanup())

describe('AuctionResultBanner', () => {
  it('names the masked winner and the final price of a sold auction', () => {
    render(AuctionResultBanner, {
      props: {
        auction: auctionDetail({
          status: 'CLOSED',
          outcome: 'SOLD',
          winnerName: 'B***',
          currentBid: '5200.00',
        }),
      },
    })

    expect(screen.getByRole('region', { name: 'Result' })).toBeTruthy()
    expect(screen.getByText('Sold to B*** for USD 5,200.00')).toBeTruthy()
  })

  it('says no bids when nobody bid', () => {
    render(AuctionResultBanner, {
      props: { auction: auctionDetail({ status: 'CLOSED', outcome: 'NO_BIDS' }) },
    })

    expect(screen.getByText('No bids')).toBeTruthy()
  })
})
