import { render, screen, waitFor } from '@testing-library/react'
import { describe, test, expect, vi, beforeEach } from 'vitest'
import { ReconciliationPanel } from './ReconciliationPanel'
import * as executionApi from '../api/execution'
import type { ReconciliationDto } from '../types'

vi.mock('../api/execution')

const mockFetchReconciliations = vi.mocked(executionApi.fetchReconciliations)

const cleanRecon: ReconciliationDto = {
  reconciliationDate: '2026-03-24',
  bookId: 'book-alpha',
  status: 'CLEAN',
  totalPositions: 5,
  matchedCount: 5,
  breakCount: 0,
  breaks: [],
  reconciledAt: '2026-03-24T18:00:00Z',
}

const reconWithBreaks: ReconciliationDto = {
  reconciliationDate: '2026-03-23',
  bookId: 'book-alpha',
  status: 'BREAKS_FOUND',
  totalPositions: 3,
  matchedCount: 2,
  breakCount: 1,
  breaks: [
    {
      instrumentId: 'AAPL',
      internalQty: '105',
      primeBrokerQty: '100',
      breakQty: '5',
      breakNotional: '750.00',
    },
  ],
  reconciledAt: '2026-03-23T18:00:00Z',
}

describe('ReconciliationPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('shows loading state initially', () => {
    mockFetchReconciliations.mockReturnValue(new Promise(() => {}))
    render(<ReconciliationPanel bookId="book-alpha" />)
    expect(screen.getByText(/loading/i)).toBeInTheDocument()
  })

  test('renders reconciliation history list', async () => {
    mockFetchReconciliations.mockResolvedValue([cleanRecon])
    render(<ReconciliationPanel bookId="book-alpha" />)

    await waitFor(() => {
      expect(screen.getByText('2026-03-24')).toBeInTheDocument()
    })
    expect(screen.getByText('CLEAN')).toBeInTheDocument()
  })

  test('shows empty state when no reconciliations exist', async () => {
    mockFetchReconciliations.mockResolvedValue([])
    render(<ReconciliationPanel bookId="book-empty" />)

    await waitFor(() => {
      expect(screen.getByText(/no reconciliation data/i)).toBeInTheDocument()
    })
  })

  test('shows empty state when bookId is null', () => {
    render(<ReconciliationPanel bookId={null} />)
    expect(screen.getByText(/select a book/i)).toBeInTheDocument()
  })

  test('shows error state when fetch fails', async () => {
    mockFetchReconciliations.mockRejectedValue(new Error('Service unavailable'))
    render(<ReconciliationPanel bookId="book-fail" />)

    await waitFor(() => {
      expect(screen.getByText(/service unavailable/i)).toBeInTheDocument()
    })
  })

  test('highlights rows with breaks in amber', async () => {
    mockFetchReconciliations.mockResolvedValue([reconWithBreaks, cleanRecon])
    render(<ReconciliationPanel bookId="book-alpha" />)

    await waitFor(() => {
      expect(screen.getByTestId('recon-row-2026-03-23')).toHaveClass('bg-amber-50')
    })
    expect(screen.getByTestId('recon-row-2026-03-24')).not.toHaveClass('bg-amber-50')
  })

  test('shows break details for BREAKS_FOUND reconciliation', async () => {
    mockFetchReconciliations.mockResolvedValue([reconWithBreaks])
    render(<ReconciliationPanel bookId="book-alpha" />)

    await waitFor(() => {
      expect(screen.getByText('AAPL')).toBeInTheDocument()
    })
    expect(screen.getByText('750.00')).toBeInTheDocument()
    expect(screen.getByTestId('break-row-AAPL')).toHaveClass('bg-amber-50')
  })

  test('does not show break details for CLEAN reconciliation', async () => {
    mockFetchReconciliations.mockResolvedValue([cleanRecon])
    render(<ReconciliationPanel bookId="book-alpha" />)

    await waitFor(() => {
      expect(screen.getByText('CLEAN')).toBeInTheDocument()
    })
    expect(screen.queryByTestId(/break-row-/)).not.toBeInTheDocument()
  })

  test('shows matched count and total position count', async () => {
    mockFetchReconciliations.mockResolvedValue([cleanRecon])
    render(<ReconciliationPanel bookId="book-alpha" />)

    await waitFor(() => {
      expect(screen.getByText('5 / 5')).toBeInTheDocument()
    })
  })
})
