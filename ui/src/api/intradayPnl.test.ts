import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchIntradayPnl } from './intradayPnl'

describe('fetchIntradayPnl', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('calls the correct endpoint with from and to parameters', async () => {
    const mockFetch = vi.mocked(fetch)
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ bookId: 'book-1', snapshots: [] }),
    } as Response)

    await fetchIntradayPnl('book-1', '2026-03-24T09:00:00Z', '2026-03-24T17:00:00Z')

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/risk/pnl/intraday/book-1?from=2026-03-24T09%3A00%3A00Z&to=2026-03-24T17%3A00%3A00Z',
    )
  })

  it('returns the parsed response on success', async () => {
    const mockFetch = vi.mocked(fetch)
    const series = { bookId: 'book-1', snapshots: [{ snapshotAt: '2026-03-24T09:30:00Z', totalPnl: '1500.00' }] }
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => series,
    } as Response)

    const result = await fetchIntradayPnl('book-1', '2026-03-24T09:00:00Z', '2026-03-24T17:00:00Z')

    expect(result).toEqual(series)
  })

  it('throws an error when the response is not ok', async () => {
    const mockFetch = vi.mocked(fetch)
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 400,
      json: async () => ({ error: 'missing from parameter' }),
    } as Response)

    await expect(
      fetchIntradayPnl('book-1', '', '2026-03-24T17:00:00Z'),
    ).rejects.toThrow('missing from parameter')
  })
})
