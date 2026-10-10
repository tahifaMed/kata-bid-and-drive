const usd = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0,
})

/** Whole-dollar display for bids, for example $81,500. */
export function formatCurrency(amount: number): string {
  return usd.format(amount)
}
