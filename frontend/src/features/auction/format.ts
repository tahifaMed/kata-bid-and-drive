const usd = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  currencyDisplay: 'code',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/** Two decimals and a fixed currency code, for example USD 81,500.00. The amount is the API string (ADR 0004). */
export function formatCurrency(amount: string): string {
  return usd.format(Number(amount))
}

const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

/** Time left until the end, for example "2d 4h", "3h 12m", "12m", or "Ending" under a minute. */
export function formatTimeLeft(endsAt: string, now: number): string {
  const remaining = Date.parse(endsAt) - now
  if (remaining < MINUTE) {
    return 'Ending'
  }
  const days = Math.floor(remaining / DAY)
  const hours = Math.floor((remaining % DAY) / HOUR)
  const minutes = Math.floor((remaining % HOUR) / MINUTE)
  if (days > 0) {
    return `${days}d ${hours}h`
  }
  if (hours > 0) {
    return `${hours}h ${minutes}m`
  }
  return `${minutes}m`
}
