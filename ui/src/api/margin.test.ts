import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchMarginEstimate } from './margin'

describe('margin API', () => {
  const mockFetch = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', mockFetch)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('GETs /api/v1/books/{bookId}/margin and returns the parsed payload on 200', async () => {
    const estimate = {
      initialMargin: '12500.00',
      variationMargin: '350.00',
      totalMargin: '12850.00',
      currency: 'USD',
    }
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve(estimate),
    })

    const result = await fetchMarginEstimate('BOOK-001')

    expect(result).toEqual(estimate)
    expect(mockFetch).toHaveBeenCalledWith('/api/v1/books/BOOK-001/margin')
  })

  it('appends previousMTM as a query parameter when supplied', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ initialMargin: '0', variationMargin: '0', totalMargin: '0', currency: 'USD' }),
    })

    await fetchMarginEstimate('BOOK-001', '9876.50')

    expect(mockFetch).toHaveBeenCalledWith('/api/v1/books/BOOK-001/margin?previousMTM=9876.50')
  })

  it('URL-encodes the bookId so special characters cannot break out of the path', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ initialMargin: '0', variationMargin: '0', totalMargin: '0', currency: 'USD' }),
    })

    await fetchMarginEstimate('book/special & id')

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/books/book%2Fspecial%20%26%20id/margin',
    )
  })

  it('returns null on a 404 so the UI can render an empty state instead of throwing', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 404,
      statusText: 'Not Found',
    })

    const result = await fetchMarginEstimate('UNKNOWN')

    expect(result).toBeNull()
  })

  it('throws with status and statusText on a 500 response', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
    })

    await expect(fetchMarginEstimate('BOOK-001')).rejects.toThrow(
      'Failed to fetch margin estimate: 500 Internal Server Error',
    )
  })
})
