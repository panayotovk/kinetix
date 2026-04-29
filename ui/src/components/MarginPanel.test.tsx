import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { MarginPanel } from './MarginPanel'
import type { UseMarginEstimateResult } from '../hooks/useMarginEstimate'

vi.mock('../hooks/useMarginEstimate', () => ({
  useMarginEstimate: vi.fn(),
}))

import { useMarginEstimate } from '../hooks/useMarginEstimate'
const mockUseMarginEstimate = useMarginEstimate as unknown as ReturnType<typeof vi.fn>

function mockHookState(overrides: Partial<UseMarginEstimateResult> = {}): UseMarginEstimateResult {
  return {
    estimate: null,
    loading: false,
    error: null,
    refresh: vi.fn(),
    ...overrides,
  }
}

describe('MarginPanel', () => {
  beforeEach(() => {
    mockUseMarginEstimate.mockReset()
  })

  it('renders the no-book empty state when no bookId is supplied', () => {
    mockUseMarginEstimate.mockReturnValue(mockHookState())

    render(<MarginPanel bookId={null} />)

    expect(screen.getByText('No book selected')).toBeInTheDocument()
    expect(screen.queryByTestId('margin-panel')).not.toBeInTheDocument()
  })

  it('renders a loading spinner while the hook reports loading', () => {
    mockUseMarginEstimate.mockReturnValue(mockHookState({ loading: true }))

    render(<MarginPanel bookId="BOOK-001" />)

    expect(screen.getByTestId('margin-loading')).toBeInTheDocument()
    expect(screen.queryByTestId('margin-panel')).not.toBeInTheDocument()
  })

  it('renders the no-data empty state when the hook returns null', () => {
    mockUseMarginEstimate.mockReturnValue(mockHookState({ estimate: null }))

    render(<MarginPanel bookId="BOOK-001" />)

    expect(screen.getByText('No margin data')).toBeInTheDocument()
  })

  it('renders the three margin summary cards with formatted currency values', () => {
    mockUseMarginEstimate.mockReturnValue(mockHookState({
      estimate: {
        initialMargin: '12500.00',
        variationMargin: '350.00',
        totalMargin: '12850.00',
        currency: 'USD',
      },
    }))

    render(<MarginPanel bookId="BOOK-001" />)

    expect(screen.getByTestId('margin-panel')).toBeInTheDocument()
    expect(screen.getByTestId('margin-initial')).toHaveTextContent('$12,500.00')
    expect(screen.getByTestId('margin-variation')).toHaveTextContent('$350.00')
    expect(screen.getByTestId('margin-total')).toHaveTextContent('$12,850.00')
  })

  it('shows the error banner with the message and a retry button when the hook reports an error', () => {
    const refresh = vi.fn()
    mockUseMarginEstimate.mockReturnValue(mockHookState({
      error: 'Failed to fetch margin estimate: 500 Internal Server Error',
      refresh,
    }))

    render(<MarginPanel bookId="BOOK-001" />)

    expect(screen.getByTestId('margin-error')).toHaveTextContent('500 Internal Server Error')

    fireEvent.click(screen.getByTestId('margin-retry-btn'))
    expect(refresh).toHaveBeenCalledTimes(1)
  })

  it('clicking the refresh button on the populated panel calls refresh', () => {
    const refresh = vi.fn()
    mockUseMarginEstimate.mockReturnValue(mockHookState({
      estimate: {
        initialMargin: '100.00',
        variationMargin: '0.00',
        totalMargin: '100.00',
        currency: 'USD',
      },
      refresh,
    }))

    render(<MarginPanel bookId="BOOK-001" />)

    fireEvent.click(screen.getByTestId('margin-refresh-btn'))
    expect(refresh).toHaveBeenCalledTimes(1)
  })
})
