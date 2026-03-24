import { renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as intradayPnlApi from '../api/intradayPnl'
import { useIntradayPnlSeries } from './useIntradayPnlSeries'

vi.mock('../api/intradayPnl')

const mockFetchIntradayPnl = vi.mocked(intradayPnlApi.fetchIntradayPnl)

const sampleSeries = {
  bookId: 'book-1',
  snapshots: [
    {
      snapshotAt: '2026-03-24T09:30:00Z',
      baseCurrency: 'USD',
      trigger: 'position_change',
      totalPnl: '1500.00',
      realisedPnl: '500.00',
      unrealisedPnl: '1000.00',
      deltaPnl: '1200.00',
      gammaPnl: '80.00',
      vegaPnl: '40.00',
      thetaPnl: '-15.00',
      rhoPnl: '7.00',
      unexplainedPnl: '188.00',
      highWaterMark: '1800.00',
    },
  ],
}

describe('useIntradayPnlSeries', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('returns empty snapshots and loading false when bookId is null', () => {
    const { result } = renderHook(() => useIntradayPnlSeries(null, '2026-03-24T09:00:00Z', '2026-03-24T17:00:00Z'))

    expect(result.current.snapshots).toEqual([])
    expect(result.current.loading).toBe(false)
    expect(result.current.error).toBeNull()
  })

  it('starts loading when bookId and range are provided', async () => {
    mockFetchIntradayPnl.mockResolvedValueOnce(sampleSeries)

    const { result } = renderHook(() =>
      useIntradayPnlSeries('book-1', '2026-03-24T09:00:00Z', '2026-03-24T17:00:00Z'),
    )

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.snapshots).toHaveLength(1)
    expect(result.current.snapshots[0].totalPnl).toBe('1500.00')
  })

  it('sets error state when fetch fails', async () => {
    mockFetchIntradayPnl.mockRejectedValueOnce(new Error('upstream failure'))

    const { result } = renderHook(() =>
      useIntradayPnlSeries('book-1', '2026-03-24T09:00:00Z', '2026-03-24T17:00:00Z'),
    )

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.error).toBe('upstream failure')
    expect(result.current.snapshots).toEqual([])
  })

  it('does not call fetchIntradayPnl when bookId is null', () => {
    renderHook(() => useIntradayPnlSeries(null, '2026-03-24T09:00:00Z', '2026-03-24T17:00:00Z'))

    expect(mockFetchIntradayPnl).not.toHaveBeenCalled()
  })
})
