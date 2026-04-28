import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchExecutionCosts, fetchReconciliations } from './execution'

describe('execution API', () => {
  const mockFetch = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', mockFetch)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('fetchExecutionCosts', () => {
    it('GETs /api/v1/execution/cost/{bookId} and returns the parsed list on 200', async () => {
      const costs = [
        { tradeId: 't-1', slippage: '12.5', fees: '1.20', total: '13.70' },
      ]
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(costs),
      })

      const result = await fetchExecutionCosts('BOOK-001')

      expect(result).toEqual(costs)
      expect(mockFetch).toHaveBeenCalledWith('/api/v1/execution/cost/BOOK-001')
    })

    it('URL-encodes the bookId so special characters cannot break out of the path', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve([]),
      })

      await fetchExecutionCosts('book/special & id')

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/execution/cost/book%2Fspecial%20%26%20id',
      )
    })

    it('returns an empty list when the upstream serves an empty array', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve([]),
      })

      const result = await fetchExecutionCosts('BOOK-001')

      expect(result).toEqual([])
    })

    it('throws with status and statusText on a non-2xx response', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
      })

      await expect(fetchExecutionCosts('BOOK-001')).rejects.toThrow(
        'Failed to fetch execution costs: 500 Internal Server Error',
      )
    })

    it('propagates a 404 response as an error rather than swallowing it', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 404,
        statusText: 'Not Found',
      })

      await expect(fetchExecutionCosts('UNKNOWN')).rejects.toThrow(
        'Failed to fetch execution costs: 404 Not Found',
      )
    })
  })

  describe('fetchReconciliations', () => {
    it('GETs /api/v1/execution/reconciliation/{bookId} and returns the parsed list on 200', async () => {
      const recs = [
        { tradeId: 't-1', side: 'INTERNAL', counterpartySide: 'EXTERNAL', status: 'MATCHED' },
      ]
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(recs),
      })

      const result = await fetchReconciliations('BOOK-001')

      expect(result).toEqual(recs)
      expect(mockFetch).toHaveBeenCalledWith('/api/v1/execution/reconciliation/BOOK-001')
    })

    it('URL-encodes the bookId', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve([]),
      })

      await fetchReconciliations('book/special & id')

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/execution/reconciliation/book%2Fspecial%20%26%20id',
      )
    })

    it('throws with status and statusText on a non-2xx response', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 503,
        statusText: 'Service Unavailable',
      })

      await expect(fetchReconciliations('BOOK-001')).rejects.toThrow(
        'Failed to fetch reconciliations: 503 Service Unavailable',
      )
    })
  })
})
