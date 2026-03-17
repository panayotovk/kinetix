import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  fetchDivisions,
  fetchDesks,
  fetchDeskSummary,
  fetchDivisionSummary,
  fetchFirmSummary,
} from './hierarchy'

describe('hierarchy API', () => {
  const mockFetch = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', mockFetch)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('fetchDivisions', () => {
    it('returns parsed JSON on 200', async () => {
      const divisions = [{ id: 'div-1', name: 'Equities', deskCount: 3 }]
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(divisions),
      })

      const result = await fetchDivisions()

      expect(result).toEqual(divisions)
      expect(mockFetch).toHaveBeenCalledWith('/api/v1/divisions')
    })

    it('throws on non-200 response', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
      })

      await expect(fetchDivisions()).rejects.toThrow(
        'Failed to fetch divisions: 500 Internal Server Error',
      )
    })
  })

  describe('fetchDesks', () => {
    it('fetches desks for a specific division', async () => {
      const desks = [{ id: 'desk-1', name: 'EU Equities', divisionId: 'div-1', bookCount: 2 }]
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(desks),
      })

      const result = await fetchDesks('div-1')

      expect(result).toEqual(desks)
      expect(mockFetch).toHaveBeenCalledWith('/api/v1/divisions/div-1/desks')
    })

    it('fetches all desks when no divisionId given', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve([]),
      })

      await fetchDesks()

      expect(mockFetch).toHaveBeenCalledWith('/api/v1/desks')
    })

    it('URL-encodes the divisionId', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve([]),
      })

      await fetchDesks('div/special & id')

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/divisions/div%2Fspecial%20%26%20id/desks',
      )
    })

    it('throws on non-200 response', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 404,
        statusText: 'Not Found',
      })

      await expect(fetchDesks('div-1')).rejects.toThrow(
        'Failed to fetch desks: 404 Not Found',
      )
    })
  })

  describe('fetchDeskSummary', () => {
    const summary = {
      bookId: 'desk-1',
      baseCurrency: 'USD',
      totalNav: { amount: '1000000.00', currency: 'USD' },
      totalUnrealizedPnl: { amount: '50000.00', currency: 'USD' },
      currencyBreakdown: [],
    }

    it('returns parsed JSON on 200', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(summary),
      })

      const result = await fetchDeskSummary('desk-1')

      expect(result).toEqual(summary)
      expect(mockFetch).toHaveBeenCalledWith('/api/v1/desks/desk-1/summary')
    })

    it('appends baseCurrency when provided', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(summary),
      })

      await fetchDeskSummary('desk-1', 'EUR')

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/desks/desk-1/summary?baseCurrency=EUR',
      )
    })

    it('throws on non-200 response', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
      })

      await expect(fetchDeskSummary('desk-1')).rejects.toThrow(
        'Failed to fetch desk summary: 500 Internal Server Error',
      )
    })
  })

  describe('fetchDivisionSummary', () => {
    const summary = {
      bookId: 'div-1',
      baseCurrency: 'USD',
      totalNav: { amount: '5000000.00', currency: 'USD' },
      totalUnrealizedPnl: { amount: '200000.00', currency: 'USD' },
      currencyBreakdown: [],
    }

    it('returns parsed JSON on 200', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(summary),
      })

      const result = await fetchDivisionSummary('div-1')

      expect(result).toEqual(summary)
      expect(mockFetch).toHaveBeenCalledWith('/api/v1/divisions/div-1/summary')
    })

    it('appends baseCurrency when provided', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(summary),
      })

      await fetchDivisionSummary('div-1', 'GBP')

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/divisions/div-1/summary?baseCurrency=GBP',
      )
    })

    it('throws on non-200 response', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 404,
        statusText: 'Not Found',
      })

      await expect(fetchDivisionSummary('div-1')).rejects.toThrow(
        'Failed to fetch division summary: 404 Not Found',
      )
    })
  })

  describe('fetchFirmSummary', () => {
    const summary = {
      bookId: 'firm',
      baseCurrency: 'USD',
      totalNav: { amount: '50000000.00', currency: 'USD' },
      totalUnrealizedPnl: { amount: '1000000.00', currency: 'USD' },
      currencyBreakdown: [],
    }

    it('returns parsed JSON on 200', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(summary),
      })

      const result = await fetchFirmSummary()

      expect(result).toEqual(summary)
      expect(mockFetch).toHaveBeenCalledWith('/api/v1/firm/summary')
    })

    it('appends baseCurrency when provided', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(summary),
      })

      await fetchFirmSummary('JPY')

      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/firm/summary?baseCurrency=JPY',
      )
    })

    it('throws on non-200 response', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 503,
        statusText: 'Service Unavailable',
      })

      await expect(fetchFirmSummary()).rejects.toThrow(
        'Failed to fetch firm summary: 503 Service Unavailable',
      )
    })
  })
})
