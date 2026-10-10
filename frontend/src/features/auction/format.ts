const usd = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  currencyDisplay: 'code',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/** Two decimals and a fixed currency code, for example USD 81,500.00. */
export function formatCurrency(amount: number): string {
  return usd.format(amount)
}
