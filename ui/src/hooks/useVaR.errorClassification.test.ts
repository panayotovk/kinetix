import { renderHook, waitFor, act } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useVaR } from './useVaR'

vi.mock('../api/risk', () => ({
  fetchVaR: vi.fn(),
  triggerVaRCalculation: vi.fn(),
}))

vi.mock('../api/jobHistory', () => ({
  fetchChartData: vi.fn().mockResolvedValue({ points: [], bucketSizeMs: 3600000 }),
}))

import { fetchVaR, triggerVaRCalculation } from '../api/risk'

const mockFetchVaR = vi.mocked(fetchVaR)
const mockTriggerVaRCalculation = vi.mocked(triggerVaRCalculation)

class FetchError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
    this.name = 'FetchError'
  }
}

describe('useVaR — errorTransient classification', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('errorTransient is false initially', async () => {
    mockFetchVaR.mockResolvedValue(null)

    const { result } = renderHook(() => useVaR('book-1'))

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(result.current.errorTransient).toBe(false)
  })

  it('500 on refresh sets errorTransient to false', async () => {
    mockFetchVaR.mockResolvedValue(null)
    mockTriggerVaRCalculation.mockRejectedValue(new FetchError(500, 'Internal Server Error'))

    const { result } = renderHook(() => useVaR('book-1'))

    await waitFor(() => expect(result.current.loading).toBe(false))

    await act(async () => {
      await result.current.refresh()
    })

    expect(result.current.errorTransient).toBe(false)
    expect(result.current.error).not.toBeNull()
  })

  it('non-retryable error classifies errorTransient as false', async () => {
    // classifyFetchError returns retryable:false for status 500
    mockFetchVaR.mockResolvedValue(null)
    mockTriggerVaRCalculation.mockRejectedValue(new FetchError(500, 'Internal Server Error'))

    const { result } = renderHook(() => useVaR('book-1'))

    await waitFor(() => expect(result.current.loading).toBe(false))

    await act(async () => {
      await result.current.refresh()
    })

    expect(result.current.errorTransient).toBe(false)
  })
})
