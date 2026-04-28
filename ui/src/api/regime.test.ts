import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchCurrentRegime, fetchRegimeHistory } from './regime'

describe('regime API', () => {
  const mockFetch = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', mockFetch)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('fetchCurrentRegime', () => {
    it('GETs /api/v1/risk/regime/current and returns the parsed payload on 200', async () => {
      const regime = {
        regime: 'NORMAL',
        confidence: 0.92,
        observedAt: '2026-04-22T10:00:00Z',
      }
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(regime),
      })

      const result = await fetchCurrentRegime()

      expect(result).toEqual(regime)
      expect(mockFetch).toHaveBeenCalledWith('/api/v1/risk/regime/current')
    })

    it('throws with status and statusText on a non-2xx response', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 503,
        statusText: 'Service Unavailable',
      })

      await expect(fetchCurrentRegime()).rejects.toThrow(
        'Failed to fetch current regime: 503 Service Unavailable',
      )
    })

    it('propagates a 404 response as an error rather than returning null', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 404,
        statusText: 'Not Found',
      })

      await expect(fetchCurrentRegime()).rejects.toThrow(
        'Failed to fetch current regime: 404 Not Found',
      )
    })
  })

  describe('fetchRegimeHistory', () => {
    it('defaults limit to 50 when not provided', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ entries: [] }),
      })

      await fetchRegimeHistory()

      expect(mockFetch).toHaveBeenCalledWith('/api/v1/risk/regime/history?limit=50')
    })

    it('uses the supplied limit value when provided', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ entries: [] }),
      })

      await fetchRegimeHistory(7)

      expect(mockFetch).toHaveBeenCalledWith('/api/v1/risk/regime/history?limit=7')
    })

    it('returns the parsed payload on 200', async () => {
      const history = {
        entries: [
          { regime: 'NORMAL', startedAt: '2026-04-20T00:00:00Z', endedAt: null },
        ],
      }
      mockFetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(history),
      })

      const result = await fetchRegimeHistory()

      expect(result).toEqual(history)
    })

    it('throws with status and statusText on a non-2xx response', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
      })

      await expect(fetchRegimeHistory()).rejects.toThrow(
        'Failed to fetch regime history: 500 Internal Server Error',
      )
    })
  })
})
