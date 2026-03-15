import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { EodDailyGrid } from './EodDailyGrid'
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

const defaultProps = {
  selectedDate: null,
  compareDates: [] as string[],
  onSelectDate: vi.fn(),
  onCompareDatesChange: vi.fn(),
  onCompare: vi.fn(),
}

describe('EodDailyGrid', () => {
  it('renders a row for each entry', () => {
    const entries = [
      makeEntry({ valuationDate: '2026-03-14' }),
      makeEntry({ valuationDate: '2026-03-13', jobId: 'job-2' }),
      makeEntry({ valuationDate: '2026-03-12', jobId: 'job-3' }),
    ]

    render(<EodDailyGrid {...defaultProps} entries={entries} />)

    expect(screen.getByTestId('eod-row-2026-03-14')).toBeInTheDocument()
    expect(screen.getByTestId('eod-row-2026-03-13')).toBeInTheDocument()
    expect(screen.getByTestId('eod-row-2026-03-12')).toBeInTheDocument()
  })

  it('renders MISSING badge for entries with null varValue', () => {
    const entries = [
      makeEntry({ valuationDate: '2026-03-14', varValue: null, promotedBy: null, promotedAt: null }),
    ]

    render(<EodDailyGrid {...defaultProps} entries={entries} />)

    expect(screen.getByTestId('missing-badge-2026-03-14')).toBeInTheDocument()
    expect(screen.getByText('MISSING')).toBeInTheDocument()
  })

  it('renders em-dash for null Greek values when hidden cols are visible', () => {
    const entries = [
      makeEntry({ delta: null, gamma: null, vega: null, theta: null, rho: null }),
    ]

    const { container } = render(<EodDailyGrid {...defaultProps} entries={entries} />)

    // Toggle hidden columns on
    const colToggle = screen.getByTestId('eod-col-toggle')
    fireEvent.click(colToggle)
    const checkbox = container.querySelector('input[type="checkbox"]')
    if (checkbox) fireEvent.click(checkbox)

    // After toggling, em-dash cells should appear
    const dashes = screen.getAllByText('\u2014')
    expect(dashes.length).toBeGreaterThan(0)
  })

  it('colour codes positive VaR DoD as red class', () => {
    const entries = [makeEntry({ varChange: 5000, varChangePct: 5.0 })]

    const { container } = render(<EodDailyGrid {...defaultProps} entries={entries} />)

    // The VaR DoD column is a <td> with the colour class applied directly
    const redCells = container.querySelectorAll('td.text-red-600')
    expect(redCells.length).toBeGreaterThan(0)
    expect(redCells[0].textContent).toMatch(/▲/)
  })

  it('colour codes negative VaR DoD as green class', () => {
    const entries = [makeEntry({ varChange: -5000, varChangePct: -5.0 })]

    const { container } = render(<EodDailyGrid {...defaultProps} entries={entries} />)

    const greenCells = container.querySelectorAll('td.text-green-600')
    expect(greenCells.length).toBeGreaterThan(0)
    expect(greenCells[0].textContent).toMatch(/▼/)
  })

  it('calls onSelectDate when a non-missing row is clicked', () => {
    const onSelectDate = vi.fn()
    const entries = [makeEntry({ valuationDate: '2026-03-14' })]

    render(<EodDailyGrid {...defaultProps} entries={entries} onSelectDate={onSelectDate} />)

    fireEvent.click(screen.getByTestId('eod-row-2026-03-14'))

    expect(onSelectDate).toHaveBeenCalledWith('2026-03-14')
  })

  it('does not call onSelectDate for missing rows', () => {
    const onSelectDate = vi.fn()
    const entries = [
      makeEntry({ valuationDate: '2026-03-14', varValue: null, promotedBy: null, promotedAt: null }),
    ]

    render(<EodDailyGrid {...defaultProps} entries={entries} onSelectDate={onSelectDate} />)

    fireEvent.click(screen.getByTestId('eod-row-2026-03-14'))

    expect(onSelectDate).not.toHaveBeenCalled()
  })

  it('shows comparison bar when compareDates has entries', () => {
    const entries = [
      makeEntry({ valuationDate: '2026-03-14' }),
      makeEntry({ valuationDate: '2026-03-13', jobId: 'job-2' }),
    ]

    render(
      <EodDailyGrid
        {...defaultProps}
        entries={entries}
        compareDates={['2026-03-14', '2026-03-13']}
      />,
    )

    expect(screen.getByTestId('eod-comparison-bar')).toBeInTheDocument()
    expect(screen.getByTestId('eod-compare-btn')).toBeInTheDocument()
  })

  it('hides comparison bar when no dates are selected', () => {
    const entries = [makeEntry()]

    render(<EodDailyGrid {...defaultProps} entries={entries} compareDates={[]} />)

    expect(screen.queryByTestId('eod-comparison-bar')).not.toBeInTheDocument()
  })

  it('shows empty state when no entries', () => {
    render(<EodDailyGrid {...defaultProps} entries={[]} />)

    expect(screen.getByTestId('eod-grid-empty')).toBeInTheDocument()
  })

  it('sorts by date when date header is clicked', () => {
    const entries = [
      makeEntry({ valuationDate: '2026-03-13', jobId: 'job-2' }),
      makeEntry({ valuationDate: '2026-03-14' }),
    ]

    render(<EodDailyGrid {...defaultProps} entries={entries} />)

    const rows = screen.getAllByRole('row').slice(1) // skip header
    // Default sort: descending by date → 2026-03-14 first
    expect(rows[0]).toHaveAttribute('data-testid', 'eod-row-2026-03-14')

    // Click date header to toggle to ascending
    fireEvent.click(screen.getByTestId('eod-sort-valuationDate'))
    const rowsAsc = screen.getAllByRole('row').slice(1)
    expect(rowsAsc[0]).toHaveAttribute('data-testid', 'eod-row-2026-03-13')
  })
})
