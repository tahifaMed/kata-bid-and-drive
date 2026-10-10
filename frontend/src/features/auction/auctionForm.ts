import type { CreateAuctionRequest } from './types'

// Client-side mirror of the backend rules in spec 004. The server stays authoritative.
export type AuctionFormField =
  | 'make'
  | 'model'
  | 'year'
  | 'mileageKm'
  | 'description'
  | 'imageUrl'
  | 'startingPrice'
  | 'minIncrement'
  | 'endsAt'

export type AuctionFormValues = Record<AuctionFormField, string>
export type AuctionFormErrors = Partial<Record<AuctionFormField, string>>

export const EMPTY_AUCTION_FORM: AuctionFormValues = {
  make: '',
  model: '',
  year: '',
  mileageKm: '',
  description: '',
  imageUrl: '',
  startingPrice: '',
  minIncrement: '',
  endsAt: '',
}

const FIRST_YEAR = 1886
const MAX_TEXT = 60
const MAX_DESCRIPTION = 2000
const MAX_URL = 2048

// Positive, at most ten integer digits, at most two decimals. Trailing zeros are allowed.
export const MONEY_PATTERN = /^(?!0+(\.0+)?$)\d{1,10}(\.\d{1,2}0*)?$/

export function validateAuctionForm(values: AuctionFormValues, now: Date): AuctionFormErrors {
  const errors: AuctionFormErrors = {}

  for (const field of ['make', 'model'] as const) {
    const text = values[field].trim()
    if (text === '') errors[field] = 'Enter a value.'
    else if (text.length > MAX_TEXT) errors[field] = `Use at most ${MAX_TEXT} characters.`
  }

  // UTC, to match the server. A browser in another zone can be on a different calendar year.
  const maxYear = now.getUTCFullYear() + 1
  if (!/^\d+$/.test(values.year)) {
    errors.year = `Enter a year from ${FIRST_YEAR} to ${maxYear}.`
  } else if (Number(values.year) < FIRST_YEAR || Number(values.year) > maxYear) {
    errors.year = `Enter a year from ${FIRST_YEAR} to ${maxYear}.`
  }

  if (!/^\d+$/.test(values.mileageKm)) errors.mileageKm = 'Enter the mileage in km, zero or more.'

  if (values.description.length > MAX_DESCRIPTION) {
    errors.description = `Use at most ${MAX_DESCRIPTION} characters.`
  }

  if (values.imageUrl !== '' && !isHttpUrl(values.imageUrl)) {
    errors.imageUrl = 'Enter a web address that starts with http:// or https://.'
  }

  for (const field of ['startingPrice', 'minIncrement'] as const) {
    if (!MONEY_PATTERN.test(values[field])) {
      errors[field] = 'Enter an amount above zero with at most two decimals, for example 12000.00.'
    }
  }

  // The end-time window needs the server clock. The browser clock can be wrong, so the server decides
  // (ADR 0008) and a rejected end time comes back as the generic error on the form.
  if (values.endsAt === '') {
    errors.endsAt = 'Choose when the auction ends.'
  }

  return errors
}

export function toCreateAuctionRequest(values: AuctionFormValues): CreateAuctionRequest {
  const description = values.description.trim()
  const imageUrl = values.imageUrl.trim()
  return {
    car: {
      make: values.make.trim(),
      model: values.model.trim(),
      year: Number(values.year),
      mileageKm: Number(values.mileageKm),
      ...(description === '' ? {} : { description }),
      ...(imageUrl === '' ? {} : { imageUrl }),
    },
    startingPrice: values.startingPrice,
    minIncrement: values.minIncrement,
    // datetime-local has no offset, so the browser reads it as local time. Send UTC.
    endsAt: new Date(values.endsAt).toISOString(),
  }
}

function isHttpUrl(value: string): boolean {
  // The server rejects whitespace, although URL() would accept and encode it.
  if (value.length > MAX_URL || /\s/.test(value)) return false
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}
