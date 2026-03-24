import { render, screen, waitFor } from '@testing-library/react'
import { describe, test, expect, vi, beforeEach } from 'vitest'
import { ExecutionCostPanel } from './ExecutionCostPanel'
import * as executionApi from '../api/execution'
import type { ExecutionCostDto } from '../types'

vi.mock('../api/execution')

const mockFetchExecutionCosts = vi.mocked(executionApi.fetchExecutionCosts)

const sampleCost: ExecutionCostDto = {
  orderId: 'ord-001',
  bookId: 'book-alpha',
  instrumentId: 'AAPL',
  completedAt: '2026-03-24T15:00:00Z',
  arrivalPrice: '150.00',
  averageFillPrice: '150.15',
  side: 'BUY',
  totalQty: '100',
  slippageBps: '10.00',
  marketImpactBps: null,
  timingCostBps: null,
  totalCostBps: '10.00',
}

describe('ExecutionCostPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('shows loading state initially', () => {
    mockFetchExecutionCosts.mockReturnValue(new Promise(() => {}))
    render(<ExecutionCostPanel bookId="book-alpha" />)
    expect(screen.getByText(/loading/i)).toBeInTheDocument()
  })

  test('renders execution cost table with slippage data', async () => {
    mockFetchExecutionCosts.mockResolvedValue([sampleCost])
    render(<ExecutionCostPanel bookId="book-alpha" />)

    await waitFor(() => {
      expect(screen.getByText('AAPL')).toBeInTheDocument()
    })
    expect(screen.getByTestId('slippage-ord-001')).toHaveTextContent('10.00')
    expect(screen.getByTestId('cost-row-ord-001')).toBeInTheDocument()
  })

  test('shows arrival price and average fill price columns', async () => {
    mockFetchExecutionCosts.mockResolvedValue([sampleCost])
    render(<ExecutionCostPanel bookId="book-alpha" />)

    await waitFor(() => {
      expect(screen.getByText('150.00')).toBeInTheDocument()
    })
    expect(screen.getByText('150.15')).toBeInTheDocument()
  })

  test('shows empty state when no execution costs exist', async () => {
    mockFetchExecutionCosts.mockResolvedValue([])
    render(<ExecutionCostPanel bookId="book-empty" />)

    await waitFor(() => {
      expect(screen.getByText(/no execution cost data/i)).toBeInTheDocument()
    })
  })

  test('shows error state when fetch fails', async () => {
    mockFetchExecutionCosts.mockRejectedValue(new Error('Network error'))
    render(<ExecutionCostPanel bookId="book-fail" />)

    await waitFor(() => {
      expect(screen.getByText(/network error/i)).toBeInTheDocument()
    })
  })

  test('shows empty state when bookId is null', () => {
    render(<ExecutionCostPanel bookId={null} />)
    expect(screen.getByText(/select a book/i)).toBeInTheDocument()
  })

  test('highlights positive slippage (cost) in amber', async () => {
    mockFetchExecutionCosts.mockResolvedValue([sampleCost])
    render(<ExecutionCostPanel bookId="book-alpha" />)

    await waitFor(() => {
      expect(screen.getByTestId('slippage-ord-001')).toHaveClass('text-amber-600')
    })
  })

  test('shows BUY side with correct label', async () => {
    mockFetchExecutionCosts.mockResolvedValue([sampleCost])
    render(<ExecutionCostPanel bookId="book-alpha" />)

    await waitFor(() => {
      expect(screen.getByTestId('side-ord-001')).toHaveTextContent('BUY')
    })
  })
})
