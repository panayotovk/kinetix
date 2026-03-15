import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { EodTrendChart } from './EodTrendChart'
import type { EodTimelineEntryDto } from '../types'

function makeEntry(overrides: Partial<EodTimelineEntryDto> = {}): EodTimelineEntryDto {
  return {
    valuationDate: '2026-03-14',
    jobId: 'job-1',
    varValue: 100000,
    expectedShortfall: 150000,
    pvValue: 5000000,
    delta: 0.5,
    gamma: 0.01,
    vega: 200,
    theta: -50,
    rho: 25,
    promotedAt: '2026-03-14T19:00:00Z',
    promotedBy: 'risk-manager',
    varChange: 5000,
    varChangePct: 5.0,
    esChange: 7500,
    calculationType: 'PARAMETRIC',
    confidenceLevel: 0.99,
    ...overrides,
  }
}

describe('EodTrendChart', () => {
  it('renders empty state when no entries provided', () => {
    render(
      <EodTrendChart
        entries={[]}
        selectedDate={null}
        onSelectDate={vi.fn()}
      />,
    )

    expect(screen.getByTestId('eod-chart-empty')).toBeInTheDocument()
    expect(screen.getByText('No EOD history for this period.')).toBeInTheDocument()
  })

  it('renders single-point notice when only one entry exists', () => {
    const entries = [makeEntry()]

    render(
      <EodTrendChart
        entries={entries}
        selectedDate={null}
        onSelectDate={vi.fn()}
      />,
    )

    expect(screen.getByTestId('eod-chart-single-point')).toBeInTheDocument()
    expect(screen.getByText(/Only one data point/)).toBeInTheDocument()
  })

  it('renders VaR and ES paths when multiple entries provided', () => {
    const entries = [
      makeEntry({ valuationDate: '2026-03-13', jobId: 'job-2' }),
      makeEntry({ valuationDate: '2026-03-14' }),
    ]

    const { container } = render(
      <EodTrendChart
        entries={entries}
        selectedDate={null}
        onSelectDate={vi.fn()}
      />,
    )

    // VaR path has indigo stroke
    const varPaths = container.querySelectorAll('path[stroke="#6366f1"]')
    expect(varPaths.length).toBeGreaterThan(0)

    // ES path has amber stroke
    const esPaths = container.querySelectorAll('path[stroke="#f59e0b"]')
    expect(esPaths.length).toBeGreaterThan(0)
  })

  it('renders gap markers for entries with null varValue', () => {
    const entries = [
      makeEntry({ valuationDate: '2026-03-12', jobId: 'job-3' }),
      makeEntry({ valuationDate: '2026-03-13', jobId: 'job-2', varValue: null, expectedShortfall: null }),
      makeEntry({ valuationDate: '2026-03-14' }),
    ]

    const { container } = render(
      <EodTrendChart
        entries={entries}
        selectedDate={null}
        onSelectDate={vi.fn()}
      />,
    )

    // Gap markers are red lines
    const redLines = container.querySelectorAll('line[stroke="#ef4444"]')
    // 2 red lines per gap marker (X shape)
    expect(redLines.length).toBe(2)
  })

  it('renders loading skeleton when isLoading is true and no entries', () => {
    render(
      <EodTrendChart
        entries={[]}
        selectedDate={null}
        onSelectDate={vi.fn()}
        isLoading={true}
      />,
    )

    expect(screen.getByRole('status', { name: 'Loading chart data' })).toBeInTheDocument()
  })

  it('does not connect line segments through gaps', () => {
    const entries = [
      makeEntry({ valuationDate: '2026-03-12', varValue: 100000 }),
      makeEntry({ valuationDate: '2026-03-13', varValue: null }),
      makeEntry({ valuationDate: '2026-03-14', varValue: 110000 }),
    ]

    const { container } = render(
      <EodTrendChart
        entries={entries}
        selectedDate={null}
        onSelectDate={vi.fn()}
      />,
    )

    // Should have 2 separate VaR path segments (one before gap, one after)
    const varPaths = container.querySelectorAll('path[stroke="#6366f1"]')
    expect(varPaths.length).toBe(2)
  })
})
