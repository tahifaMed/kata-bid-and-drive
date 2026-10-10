import { describe, expect, it } from 'vitest'
import {
  EMPTY_AUCTION_FORM,
  toCreateAuctionRequest,
  validateAuctionForm,
  type AuctionFormValues,
} from '../auctionForm'

// Local time, to match how datetime-local values are read.
const NOW = new Date(2026, 9, 10, 12, 0)

function valid(overrides: Partial<AuctionFormValues> = {}): AuctionFormValues {
  return {
    ...EMPTY_AUCTION_FORM,
    make: 'Volkswagen',
    model: 'Golf',
    year: '2020',
    mileageKm: '42000',
    startingPrice: '12000.00',
    minIncrement: '100.00',
    endsAt: '2026-10-12T18:00',
    ...overrides,
  }
}

describe('validateAuctionForm', () => {
  it('accepts a complete valid form', () => {
    expect(validateAuctionForm(valid(), NOW)).toEqual({})
  })

  it('rejects a year before 1886 and accepts 1886', () => {
    expect(validateAuctionForm(valid({ year: '1885' }), NOW).year).toBeDefined()
    expect(validateAuctionForm(valid({ year: '1886' }), NOW).year).toBeUndefined()
  })

  it('accepts next year and rejects the year after', () => {
    expect(validateAuctionForm(valid({ year: '2027' }), NOW).year).toBeUndefined()
    expect(validateAuctionForm(valid({ year: '2028' }), NOW).year).toBeDefined()
  })

  it.each(['0', '0.00', '-1', '1e3', '12000.001', '1.'])('rejects the price %s', (price) => {
    expect(validateAuctionForm(valid({ startingPrice: price }), NOW).startingPrice).toBeDefined()
  })

  it.each(['12000', '12000.5', '12000.00', '12000.000'])('accepts the price %s', (price) => {
    expect(validateAuctionForm(valid({ startingPrice: price }), NOW).startingPrice).toBeUndefined()
  })

  it('requires an end time and leaves the window to the server', () => {
    expect(validateAuctionForm(valid({ endsAt: '' }), NOW).endsAt).toBeDefined()
    expect(validateAuctionForm(valid({ endsAt: '2020-01-01T10:00' }), NOW).endsAt).toBeUndefined()
  })

  it('checks the year against the UTC calendar year', () => {
    const lateDecember = new Date('2026-12-31T23:30:00Z')

    expect(validateAuctionForm(valid({ year: '2027' }), lateDecember).year).toBeUndefined()
    expect(validateAuctionForm(valid({ year: '2028' }), lateDecember).year).toBeDefined()
  })

  it('accepts only http and https photo links', () => {
    expect(validateAuctionForm(valid({ imageUrl: 'https://example.com/car.jpg' }), NOW).imageUrl).toBeUndefined()
    expect(validateAuctionForm(valid({ imageUrl: 'ftp://example.com/car.jpg' }), NOW).imageUrl).toBeDefined()
    expect(validateAuctionForm(valid({ imageUrl: 'not a link' }), NOW).imageUrl).toBeDefined()
    expect(validateAuctionForm(valid({ imageUrl: 'https://example.com/my photo.jpg' }), NOW).imageUrl).toBeDefined()
  })

  it('requires make and limits it to 60 characters', () => {
    expect(validateAuctionForm(valid({ make: '   ' }), NOW).make).toBeDefined()
    expect(validateAuctionForm(valid({ make: 'x'.repeat(61) }), NOW).make).toBeDefined()
  })
})

describe('toCreateAuctionRequest', () => {
  it('sends numbers for car fields, strings for money and the end time in UTC', () => {
    const request = toCreateAuctionRequest(valid({ description: '  Blue  ', endsAt: '2026-10-12T18:00' }))

    expect(request.car).toEqual({ make: 'Volkswagen', model: 'Golf', year: 2020, mileageKm: 42000, description: 'Blue' })
    expect(request.startingPrice).toBe('12000.00')
    expect(request.endsAt).toBe(new Date('2026-10-12T18:00').toISOString())
  })

  it('leaves out optional fields that are empty', () => {
    expect(toCreateAuctionRequest(valid()).car).not.toHaveProperty('description')
    expect(toCreateAuctionRequest(valid()).car).not.toHaveProperty('imageUrl')
  })
})
