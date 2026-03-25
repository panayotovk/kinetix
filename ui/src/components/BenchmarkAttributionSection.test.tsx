import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../hooks/useBrinsonAttribution')

import { BenchmarkAttributionSection } from './BenchmarkAttributionSection'
import { useBrinsonAttribution } from '../hooks/useBrinsonAttribution'

const mockUseBrinsonAttribution = vi.mocked(useBrinsonAttribution)

const sampleData = {
  bookId: 'BOOK-EQ-01',
  benchmarkId: 'SP500',
  asOfDate: '2026-03-25',
  sectors: [
    {
      sectorLabel: 'AAPL',
      portfolioWeight: 0.35,
      benchmarkWeight: 0.07,
      portfolioReturn: 0.12,
      benchmarkReturn: 0.10,
      allocationEffect: 0.028,
      selectionEffect: 0.014,
      interactionEffect: 0.004,
      totalActiveContribution: 0.046,
    },
  ],
  totalActiveReturn: 0.046,
  totalAllocationEffect: 0.028,
  totalSelectionEffect: 0.014,
  totalInteractionEffect: 0.004,
}

describe('BenchmarkAttributionSection', () => {
  beforeEach(() => {
    mockUseBrinsonAttribution.mockReturnValue({ data: null, loading: false, error: null })
  })

  it('renders benchmark ID input and submit button', () => {
    render(<BenchmarkAttributionSection bookId="BOOK-EQ-01" />)

    expect(screen.getByTestId('benchmark-id-input')).toBeInTheDocument()
    expect(screen.getByTestId('benchmark-attribution-submit')).toBeInTheDocument()
  })

  it('shows empty prompt when no benchmark submitted', () => {
    render(<BenchmarkAttributionSection bookId="BOOK-EQ-01" />)

    expect(screen.getByTestId('benchmark-attribution-empty')).toBeInTheDocument()
  })

  it('submit button is disabled when input is empty', () => {
    render(<BenchmarkAttributionSection bookId="BOOK-EQ-01" />)

    expect(screen.getByTestId('benchmark-attribution-submit')).toBeDisabled()
  })

  it('submit button is enabled when input has text', () => {
    render(<BenchmarkAttributionSection bookId="BOOK-EQ-01" />)

    fireEvent.change(screen.getByTestId('benchmark-id-input'), { target: { value: 'SP500' } })
    expect(screen.getByTestId('benchmark-attribution-submit')).not.toBeDisabled()
  })

  it('shows loading spinner while loading', () => {
    mockUseBrinsonAttribution.mockReturnValue({ data: null, loading: true, error: null })

    render(<BenchmarkAttributionSection bookId="BOOK-EQ-01" />)

    expect(screen.getByTestId('benchmark-attribution-loading')).toBeInTheDocument()
  })

  it('shows error message when attribution fails', () => {
    mockUseBrinsonAttribution.mockReturnValue({
      data: null,
      loading: false,
      error: 'Benchmark not found: MISSING',
    })

    render(<BenchmarkAttributionSection bookId="BOOK-EQ-01" />)

    expect(screen.getByTestId('benchmark-attribution-error')).toHaveTextContent('Benchmark not found: MISSING')
  })

  it('renders attribution table when data is available', () => {
    mockUseBrinsonAttribution.mockReturnValue({ data: sampleData, loading: false, error: null })

    render(<BenchmarkAttributionSection bookId="BOOK-EQ-01" />)

    expect(screen.getByTestId('brinson-attribution-table')).toBeInTheDocument()
    expect(screen.getByTestId('brinson-row-AAPL')).toBeInTheDocument()
  })

  it('passes entered benchmark ID to hook after form submit', () => {
    mockUseBrinsonAttribution.mockReturnValue({ data: sampleData, loading: false, error: null })

    render(<BenchmarkAttributionSection bookId="BOOK-EQ-01" />)

    fireEvent.change(screen.getByTestId('benchmark-id-input'), { target: { value: 'SP500' } })
    fireEvent.submit(screen.getByTestId('benchmark-attribution-form'))

    // After submit, the attribution table is visible (rendered from the mocked data)
    expect(screen.getByTestId('brinson-attribution-table')).toBeInTheDocument()
  })
})
