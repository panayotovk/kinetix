import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ScenarioLibraryGrid } from './ScenarioLibraryGrid'
import type { StressScenarioDto } from '../types'

const scenarios: StressScenarioDto[] = [
  {
    id: '1',
    name: 'GFC 2008',
    description: 'Global Financial Crisis',
    shocks: '{}',
    status: 'APPROVED',
    createdBy: 'system',
    approvedBy: 'admin',
    approvedAt: '2025-01-01T00:00:00Z',
    createdAt: '2025-01-01T00:00:00Z',
    scenarioType: 'HISTORICAL_REPLAY',
  },
  {
    id: '2',
    name: 'COVID 2020',
    description: 'Covid crash',
    shocks: '{}',
    status: 'APPROVED',
    createdBy: 'system',
    approvedBy: 'admin',
    approvedAt: '2025-01-02T00:00:00Z',
    createdAt: '2025-01-02T00:00:00Z',
    scenarioType: 'HISTORICAL_REPLAY',
  },
  {
    id: '3',
    name: 'Rates Shock +200bp',
    description: 'Parametric rates shock',
    shocks: '{}',
    status: 'DRAFT',
    createdBy: 'user',
    approvedBy: null,
    approvedAt: null,
    createdAt: '2025-01-03T00:00:00Z',
    scenarioType: 'PARAMETRIC',
  },
  {
    id: '4',
    name: 'Max Loss Finder',
    description: 'Reverse stress',
    shocks: '{}',
    status: 'PENDING_APPROVAL',
    createdBy: 'quant',
    approvedBy: null,
    approvedAt: null,
    createdAt: '2025-01-04T00:00:00Z',
    scenarioType: 'REVERSE_STRESS',
  },
]

describe('ScenarioLibraryGrid', () => {
  it('renders all scenarios by default', () => {
    render(<ScenarioLibraryGrid scenarios={scenarios} loading={false} error={null} />)
    expect(screen.getByTestId('scenario-library-grid')).toBeDefined()
    expect(screen.getByText('GFC 2008')).toBeDefined()
    expect(screen.getByText('COVID 2020')).toBeDefined()
    expect(screen.getByText('Rates Shock +200bp')).toBeDefined()
    expect(screen.getByText('Max Loss Finder')).toBeDefined()
  })

  it('filters scenarios by search term', async () => {
    render(<ScenarioLibraryGrid scenarios={scenarios} loading={false} error={null} />)
    const searchInput = screen.getByTestId('scenario-library-search')
    await userEvent.type(searchInput, 'GFC')
    expect(screen.getByText('GFC 2008')).toBeDefined()
    expect(screen.queryByText('COVID 2020')).toBeNull()
    expect(screen.queryByText('Rates Shock +200bp')).toBeNull()
  })

  it('filters by type PARAMETRIC', async () => {
    render(<ScenarioLibraryGrid scenarios={scenarios} loading={false} error={null} />)
    const typeFilter = screen.getByTestId('scenario-library-type-filter')
    await userEvent.selectOptions(typeFilter, 'PARAMETRIC')
    expect(screen.getByText('Rates Shock +200bp')).toBeDefined()
    expect(screen.queryByText('GFC 2008')).toBeNull()
    expect(screen.queryByText('COVID 2020')).toBeNull()
    expect(screen.queryByText('Max Loss Finder')).toBeNull()
  })

  it('filters by type HISTORICAL_REPLAY', async () => {
    render(<ScenarioLibraryGrid scenarios={scenarios} loading={false} error={null} />)
    const typeFilter = screen.getByTestId('scenario-library-type-filter')
    await userEvent.selectOptions(typeFilter, 'HISTORICAL_REPLAY')
    expect(screen.getByText('GFC 2008')).toBeDefined()
    expect(screen.getByText('COVID 2020')).toBeDefined()
    expect(screen.queryByText('Rates Shock +200bp')).toBeNull()
  })

  it('filters by type REVERSE_STRESS', async () => {
    render(<ScenarioLibraryGrid scenarios={scenarios} loading={false} error={null} />)
    const typeFilter = screen.getByTestId('scenario-library-type-filter')
    await userEvent.selectOptions(typeFilter, 'REVERSE_STRESS')
    expect(screen.getByText('Max Loss Finder')).toBeDefined()
    expect(screen.queryByText('GFC 2008')).toBeNull()
  })

  it('sorts by name ascending by default', () => {
    render(<ScenarioLibraryGrid scenarios={scenarios} loading={false} error={null} />)
    const rows = screen.getAllByTestId('scenario-library-row')
    expect(rows[0]).toHaveTextContent('COVID 2020')
    expect(rows[1]).toHaveTextContent('GFC 2008')
    expect(rows[2]).toHaveTextContent('Max Loss Finder')
    expect(rows[3]).toHaveTextContent('Rates Shock +200bp')
  })

  it('sorts by name descending when name header clicked', async () => {
    render(<ScenarioLibraryGrid scenarios={scenarios} loading={false} error={null} />)
    // Default is name/asc. One click on name toggles to desc.
    await userEvent.click(screen.getByTestId('sort-by-name'))
    const rows = screen.getAllByTestId('scenario-library-row')
    expect(rows[0]).toHaveTextContent('Rates Shock +200bp')
  })

  it('sorts by status', async () => {
    render(<ScenarioLibraryGrid scenarios={scenarios} loading={false} error={null} />)
    await userEvent.click(screen.getByTestId('sort-by-status'))
    const rows = screen.getAllByTestId('scenario-library-row')
    // APPROVED < DRAFT < PENDING_APPROVAL < REVERSE_STRESS alphabetically
    expect(rows[0]).toHaveTextContent('APPROVED')
  })

  it('sorts by type', async () => {
    render(<ScenarioLibraryGrid scenarios={scenarios} loading={false} error={null} />)
    await userEvent.click(screen.getByTestId('sort-by-type'))
    const rows = screen.getAllByTestId('scenario-library-row')
    // HISTORICAL_REPLAY < PARAMETRIC < REVERSE_STRESS alphabetically
    // The badge renders the human-readable label "Historical Replay"
    expect(rows[0]).toHaveTextContent('Historical Replay')
  })

  it('shows loading state', () => {
    render(<ScenarioLibraryGrid scenarios={[]} loading={true} error={null} />)
    expect(screen.getByTestId('scenario-library-loading')).toBeDefined()
  })

  it('shows error state', () => {
    render(
      <ScenarioLibraryGrid scenarios={[]} loading={false} error="Failed to load" />,
    )
    expect(screen.getByTestId('scenario-library-error')).toBeDefined()
    expect(screen.getByText('Failed to load')).toBeDefined()
  })

  it('shows empty state when no scenarios match filter', async () => {
    render(<ScenarioLibraryGrid scenarios={scenarios} loading={false} error={null} />)
    const searchInput = screen.getByTestId('scenario-library-search')
    await userEvent.type(searchInput, 'NONEXISTENT_SCENARIO_XYZ')
    expect(screen.getByTestId('scenario-library-empty')).toBeDefined()
  })

  it('shows type badge for each scenario', () => {
    render(<ScenarioLibraryGrid scenarios={scenarios} loading={false} error={null} />)
    expect(screen.getAllByTestId('scenario-type-badge').length).toBe(4)
  })
})
