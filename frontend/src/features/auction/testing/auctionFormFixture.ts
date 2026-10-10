import { fireEvent, screen } from '@testing-library/vue'

/** A local datetime-local value (no offset) for the given number of hours from now. */
export function localEndsIn(hours: number): string {
  const date = new Date(Date.now() + hours * 60 * 60 * 1000)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** Fills every field with valid data. */
export async function fillValidAuctionForm(): Promise<void> {
  await fireEvent.update(screen.getByLabelText('Make'), 'Volkswagen')
  await fireEvent.update(screen.getByLabelText('Model'), 'Golf')
  await fireEvent.update(screen.getByLabelText('Year'), '2020')
  await fireEvent.update(screen.getByLabelText('Mileage (km)'), '42000')
  await fireEvent.update(screen.getByLabelText('Starting price'), '12000.00')
  await fireEvent.update(screen.getByLabelText('Minimum bid increment'), '100.00')
  await fireEvent.update(screen.getByLabelText('Auction ends'), localEndsIn(48))
}
