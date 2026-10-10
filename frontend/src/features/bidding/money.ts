// The server's rule for an amount sent as text: positive, at most ten integer digits, at most two decimals
// (trailing zeros are allowed).
const AMOUNT = /^(\d{1,10})(?:\.(\d{1,2})0*)?$/

/** The amount in whole cents, or null when the server would reject it. Cents compare exactly, floats do not. */
export function toCents(amount: string): number | null {
  const match = AMOUNT.exec(amount)
  if (!match) return null
  const cents = Number(match[1]) * 100 + Number((match[2] ?? '').padEnd(2, '0'))
  return cents > 0 ? cents : null
}
