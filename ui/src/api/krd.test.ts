import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fetchKrd } from './krd'

beforeEach(() => {
  vi.restoreAllMocks()
})

describe('fetchKrd', () => {
  it('returns KRD data on success', async () => {
    const mockResponse = {
      bookId: 'port-1',
      instruments: [
        { instrumentId: 'BOND-1', krdBuckets: [{ tenorLabel: '2Y', tenorDays: 730, dv01: '120.50' }], totalDv01: '120.50' },
      ],
      aggregated: [{ tenorLabel: '2Y', tenorDays: 730, dv01: '120.50' }],
    }
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    })

    const result = await fetchKrd('port-1')
    expect(result.bookId).toBe('port-1')
    expect(result.aggregated).toHaveLength(1)
    expect(result.aggregated[0].tenorLabel).toBe('2Y')
  })

  it('throws on non-OK response', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
      json: () => Promise.resolve({ error: 'Not found' }),
    })

    await expect(fetchKrd('missing')).rejects.toThrow('Not found')
  })

  it('handles null error body gracefully', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.resolve(null),
    })

    await expect(fetchKrd('port-1')).rejects.toThrow('Failed to fetch KRD: 500')
  })
})
