import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchLimits } from './limits'

describe('limits API', () => {
  const mockFetch = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', mockFetch)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('GETs /api/v1/limits and returns the parsed list on 200', async () => {
    const limits = [
      {
        id: 'l-1',
        level: 'FIRM',
        entityId: 'firm-1',
        limitType: 'VAR',
        limitValue: '1000000',
        intradayLimit: null,
        overnightLimit: null,
        active: true,
      },
    ]
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve(limits),
    })

    const result = await fetchLimits()

    expect(result).toEqual(limits)
    expect(mockFetch).toHaveBeenCalledWith('/api/v1/limits')
  })

  it('returns an empty list when no limits are defined', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve([]),
    })

    const result = await fetchLimits()

    expect(result).toEqual([])
  })

  it('throws with status and statusText on a 500 response', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
    })

    await expect(fetchLimits()).rejects.toThrow(
      'Failed to fetch limits: 500 Internal Server Error',
    )
  })

  it('throws with status and statusText on a 503 response', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 503,
      statusText: 'Service Unavailable',
    })

    await expect(fetchLimits()).rejects.toThrow(
      'Failed to fetch limits: 503 Service Unavailable',
    )
  })
})
