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

  it('ignores a slower response for an auction it no longer asks for', async () => {
    let resolveFirst: (auction: ReturnType<typeof auctionDetail>) => void = () => {}
    vi.mocked(fetchAuction).mockReturnValueOnce(new Promise((resolve) => (resolveFirst = resolve)))
    vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail({ id: 6 }))
    const detail = useAuctionDetail()

    const first = detail.load('5')
    await detail.load('6')
    resolveFirst(auctionDetail({ id: 5 }))
    await first

    expect(detail.auction.value?.id).toBe(6)
    expect(detail.state.value).toBe('ready')
  })

  it('ignores a slower failure for an auction it no longer asks for', async () => {
    let rejectFirst: (error: Error) => void = () => {}
    vi.mocked(fetchAuction).mockReturnValueOnce(new Promise((_, reject) => (rejectFirst = reject)))
    vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail({ id: 6 }))
    const detail = useAuctionDetail()

    const first = detail.load('5')
    await detail.load('6')
    rejectFirst(new ApiError(404, 'Not found'))
    await first

    expect(detail.state.value).toBe('ready')
  })

  it.each(['9007199254740993', '99999999999999999999999'])(
    'reports not found for %s, which is too large to be an id, without calling the API',
    async (id) => {
      const detail = useAuctionDetail()

      await detail.load(id)

      expect(detail.state.value).toBe('not-found')
      expect(fetchAuction).not.toHaveBeenCalled()
    },
  )

  it('retries the auction it last asked for', async () => {
    vi.mocked(fetchAuction).mockRejectedValueOnce(new TypeError('Network down'))
    vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail({ id: 7 }))
    const detail = useAuctionDetail()
    await detail.load('7')

    await detail.retry()

    expect(fetchAuction).toHaveBeenLastCalledWith(7)
    expect(detail.state.value).toBe('ready')
  })

  it('refreshes the auction without going back to loading', async () => {
    vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail())
    let answer: (auction: ReturnType<typeof auctionDetail>) => void = () => {}
    vi.mocked(fetchAuction).mockReturnValueOnce(new Promise((resolve) => (answer = resolve)))
    const detail = useAuctionDetail()
    await detail.load('5')

    const refreshed = detail.refresh()
    expect(detail.state.value).toBe('ready')
    expect(detail.auction.value?.bidCount).toBe(0)
    answer(auctionDetail({ bidCount: 1, currentBid: '12000.00', minNextBid: '12100.00' }))
    await refreshed

    expect(fetchAuction).toHaveBeenLastCalledWith(5)
    expect(detail.state.value).toBe('ready')
    expect(detail.auction.value?.bidCount).toBe(1)
  })

  it('keeps the last auction shown when a refresh fails', async () => {
    vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail())
    vi.mocked(fetchAuction).mockRejectedValueOnce(new TypeError('Network down'))
    const detail = useAuctionDetail()
    await detail.load('5')

    await detail.refresh()

    expect(detail.state.value).toBe('ready')
    expect(detail.auction.value?.id).toBe(5)
  })

  it('drops a refresh that answers after the page moved to another auction', async () => {
    vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail())
    let answerRefresh: (auction: ReturnType<typeof auctionDetail>) => void = () => {}
    vi.mocked(fetchAuction).mockReturnValueOnce(new Promise((resolve) => (answerRefresh = resolve)))
    vi.mocked(fetchAuction).mockResolvedValueOnce(auctionDetail({ id: 6 }))
    const detail = useAuctionDetail()
    await detail.load('5')

    const refreshed = detail.refresh()
    await detail.load('6')
    answerRefresh(auctionDetail({ id: 5, bidCount: 1 }))
    await refreshed

    expect(detail.auction.value?.id).toBe(6)
  })
})
