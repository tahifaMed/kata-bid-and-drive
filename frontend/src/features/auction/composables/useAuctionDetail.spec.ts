import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../api/client'
import { fetchAuction } from '../api'
import { auctionDetail } from '../testing/auctionDetailFixture'
import { useAuctionDetail } from './useAuctionDetail'

vi.mock('../api', () => ({ fetchAuction: vi.fn<typeof fetchAuction>() }))

beforeEach(() => {
  vi.mocked(fetchAuction).mockReset()
})

describe('useAuctionDetail', () => {
  it('is loading until the auction arrives', () => {
    vi.mocked(fetchAuction).mockReturnValue(new Promise(() => {}))
    const detail = useAuctionDetail()

    void detail.load('5')

    expect(detail.state.value).toBe('loading')
  })

  it('holds the auction the API returns', async () => {
    vi.mocked(fetchAuction).mockResolvedValue(auctionDetail())
    const detail = useAuctionDetail()

    await detail.load('5')

    expect(fetchAuction).toHaveBeenCalledWith(5)
    expect(detail.state.value).toBe('ready')
    expect(detail.auction.value?.car.make).toBe('Volkswagen')
  })

  it('reports not found when the API answers 404', async () => {
    vi.mocked(fetchAuction).mockRejectedValue(new ApiError(404, 'Not found'))
    const detail = useAuctionDetail()

    await detail.load('99')

    expect(detail.state.value).toBe('not-found')
  })

  it('reports not found for an id that is not a whole number, without calling the API', async () => {
    const detail = useAuctionDetail()

    await detail.load('abc')

    expect(detail.state.value).toBe('not-found')
    expect(fetchAuction).not.toHaveBeenCalled()
  })

  it('reports an error for any other failure', async () => {
    vi.mocked(fetchAuction).mockRejectedValue(new ApiError(500, 'Server error'))
    const detail = useAuctionDetail()

    await detail.load('5')

    expect(detail.state.value).toBe('error')
  })

  it('retries the auction it last asked for', async () => {
    vi.mocked(fetchAuction).mockRejectedValueOnce(new TypeError('Network down'))
    vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail({ id: 7 }))
    const detail = useAuctionDetail()
    await detail.load('7')

    await detail.retry()

    expect(fetchAuction).toHaveBeenLastCalledWith(7)
    expect(detail.state.value).toBe('ready')
  })
})
