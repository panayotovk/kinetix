import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach, vi } from 'vitest'
import { useHierarchySummary } from './useHierarchySummary'

vi.mock('../api/bookSummary', () => ({
  fetchBookSummary: vi.fn(),
}))

vi.mock('../api/hierarchy', () => ({
  fetchFirmSummary: vi.fn(),
  fetchDivisionSummary: vi.fn(),
  fetchDeskSummary: vi.fn(),
}))

import { fetchBookSummary } from '../api/bookSummary'
import { fetchFirmSummary, fetchDivisionSummary, fetchDeskSummary } from '../api/hierarchy'

const mockFetchBookSummary = vi.mocked(fetchBookSummary)
const mockFetchFirmSummary = vi.mocked(fetchFirmSummary)
const mockFetchDivisionSummary = vi.mocked(fetchDivisionSummary)
const mockFetchDeskSummary = vi.mocked(fetchDeskSummary)

const SUMMARY = {
  bookId: 'test',
  baseCurrency: 'USD',
  totalNav: { amount: '1000000.00', currency: 'USD' },
  totalUnrealizedPnl: { amount: '50000.00', currency: 'USD' },
  currencyBreakdown: [],
}

describe('useHierarchySummary', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockFetchFirmSummary.mockResolvedValue(SUMMARY)
    mockFetchDivisionSummary.mockResolvedValue(SUMMARY)
    mockFetchDeskSummary.mockResolvedValue(SUMMARY)
    mockFetchBookSummary.mockResolvedValue(SUMMARY)
  })

  it('fetches firm summary at firm level', async () => {
    const { result } = renderHook(() =>
      useHierarchySummary({ level: 'firm', divisionId: null, deskId: null, bookId: null }),
    )

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(mockFetchFirmSummary).toHaveBeenCalledWith('USD')
    expect(result.current.summary).toEqual(SUMMARY)
    expect(result.current.summaryLabel).toBe('Firm Summary')
  })

  it('fetches division summary at division level', async () => {
    const { result } = renderHook(() =>
      useHierarchySummary({ level: 'division', divisionId: 'div-1', deskId: null, bookId: null }),
    )

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(mockFetchDivisionSummary).toHaveBeenCalledWith('div-1', 'USD')
    expect(result.current.summaryLabel).toBe('Division Summary')
  })

  it('fetches desk summary at desk level', async () => {
    const { result } = renderHook(() =>
      useHierarchySummary({ level: 'desk', divisionId: 'div-1', deskId: 'desk-1', bookId: null }),
    )

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(mockFetchDeskSummary).toHaveBeenCalledWith('desk-1', 'USD')
    expect(result.current.summaryLabel).toBe('Desk Summary')
  })

  it('fetches book summary at book level', async () => {
    const { result } = renderHook(() =>
      useHierarchySummary({ level: 'book', divisionId: 'div-1', deskId: 'desk-1', bookId: 'book-1' }),
    )

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(mockFetchBookSummary).toHaveBeenCalledWith('book-1', 'USD')
    expect(result.current.summaryLabel).toBe('Book Summary')
  })

  it('sets error on fetch failure', async () => {
    mockFetchFirmSummary.mockRejectedValue(new Error('Network error'))

    const { result } = renderHook(() =>
      useHierarchySummary({ level: 'firm', divisionId: null, deskId: null, bookId: null }),
    )

    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })

    expect(result.current.error).toBe('Network error')
    expect(result.current.summary).toBeNull()
  })
})
