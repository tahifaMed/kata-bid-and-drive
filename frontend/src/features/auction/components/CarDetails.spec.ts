import { cleanup, render, screen } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'
import { auctionDetail } from '../testing/auctionDetailFixture'
import CarDetails from './CarDetails.vue'

afterEach(() => cleanup())

describe('CarDetails', () => {
  it('shows the car title, mileage, description and seller', () => {
    const { car, seller } = auctionDetail()
    render(CarDetails, { props: { car, sellerName: seller.displayName } })

    expect(screen.getByRole('heading', { level: 1, name: '2020 Volkswagen Golf' })).toBeTruthy()
    expect(screen.getByText(/42,000 km/)).toBeTruthy()
    expect(screen.getByText('Blue, one owner')).toBeTruthy()
    expect(screen.getByText(/Alice/)).toBeTruthy()
  })

  it('shows the photo with the car title as alt text', () => {
    const { car } = auctionDetail()
    render(CarDetails, { props: { car, sellerName: 'Alice' } })

    expect(screen.getByRole('img', { name: '2020 Volkswagen Golf' }).getAttribute('src')).toBe(
      'https://example.com/golf.jpg',
    )
  })

  it('shows the neutral placeholder when there is no photo', () => {
    const car = { ...auctionDetail().car, imageUrl: null }
    const { container } = render(CarDetails, { props: { car, sellerName: 'Alice' } })

    expect(container.querySelector('img')?.getAttribute('src')).toBe(
      '/images/auctions/no-photo.svg',
    )
  })
})
