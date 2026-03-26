import { describe, it, expect, vi, beforeEach } from 'vitest'
import { authFetch, setAuthToken } from './authFetch'

describe('authFetch', () => {
  beforeEach(() => {
    setAuthToken(null)
    vi.restoreAllMocks()
  })

  it('adds Authorization header when token is set', async () => {
    setAuthToken('test-jwt-token')
    const mockResponse = new Response('{}', { status: 200 })
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockResponse)

    await authFetch('/api/v1/books')

    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/books',
      expect.objectContaining({
        headers: expect.any(Headers),
      }),
    )
    const calledHeaders = (vi.mocked(globalThis.fetch).mock.calls[0][1]?.headers as Headers)
    expect(calledHeaders.get('Authorization')).toBe('Bearer test-jwt-token')
  })

  it('does not add Authorization header when no token is set', async () => {
    const mockResponse = new Response('{}', { status: 200 })
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockResponse)

    await authFetch('/api/v1/books')

    // When no token, authFetch passes through to fetch without modifying args
    expect(globalThis.fetch).toHaveBeenCalledWith('/api/v1/books')
    expect(vi.mocked(globalThis.fetch).mock.calls[0]).toHaveLength(1)
  })

  it('preserves existing headers alongside Authorization', async () => {
    setAuthToken('my-token')
    const mockResponse = new Response('{}', { status: 200 })
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockResponse)

    await authFetch('/api/v1/risk', {
      headers: { 'Content-Type': 'application/json' },
    })

    const calledHeaders = (vi.mocked(globalThis.fetch).mock.calls[0][1]?.headers as Headers)
    expect(calledHeaders.get('Authorization')).toBe('Bearer my-token')
    expect(calledHeaders.get('Content-Type')).toBe('application/json')
  })
})
