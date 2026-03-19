import { renderHook, act } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useWhatIf } from './useWhatIf'

vi.mock('../api/whatIf', () => ({
  FetchError: class FetchError extends Error {},
  runWhatIfAnalysis: vi.fn().mockResolvedValue(null),
}))

const STORAGE_KEY = 'kinetix:whatif:trades'

describe('useWhatIf — sessionStorage persistence', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.resetAllMocks()
  })

  it('persists trades to sessionStorage on update', () => {
    const { result } = renderHook(() => useWhatIf('book-1'))

    act(() => {
      result.current.updateTrade(0, 'instrumentId', 'AAPL')
    })

    const stored = JSON.parse(sessionStorage.getItem(STORAGE_KEY)!)
    expect(stored).toHaveLength(1)
    expect(stored[0].instrumentId).toBe('AAPL')
  })

  it('initializes trades from sessionStorage on mount', () => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify([
      {
        instrumentId: 'GOOGL',
        assetClass: 'EQUITY',
        side: 'BUY',
        quantity: '50',
        priceAmount: '2800',
        priceCurrency: 'USD',
      },
    ]))

    const { result } = renderHook(() => useWhatIf('book-1'))

    expect(result.current.trades).toHaveLength(1)
    expect(result.current.trades[0].instrumentId).toBe('GOOGL')
  })

  it('trades survive a remount (unmount + re-render)', () => {
    const { result, unmount } = renderHook(() => useWhatIf('book-1'))

    act(() => {
      result.current.updateTrade(0, 'instrumentId', 'MSFT')
      result.current.updateTrade(0, 'quantity', '200')
    })

    unmount()

    const { result: result2 } = renderHook(() => useWhatIf('book-1'))

    expect(result2.current.trades[0].instrumentId).toBe('MSFT')
    expect(result2.current.trades[0].quantity).toBe('200')
  })

  it('reset() reverts trades to empty state and sessionStorage no longer has previous data', () => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify([
      {
        instrumentId: 'SPY',
        assetClass: 'EQUITY',
        side: 'SELL',
        quantity: '10',
        priceAmount: '450',
        priceCurrency: 'USD',
      },
    ]))

    const { result } = renderHook(() => useWhatIf('book-1'))

    act(() => {
      result.current.reset()
    })

    // After reset, trades are reverted to a single empty trade
    expect(result.current.trades).toHaveLength(1)
    expect(result.current.trades[0].instrumentId).toBe('')

    // sessionStorage should not contain the old trade data
    const stored = JSON.parse(sessionStorage.getItem(STORAGE_KEY) ?? '[]')
    expect(stored[0].instrumentId).toBe('')
  })
})
