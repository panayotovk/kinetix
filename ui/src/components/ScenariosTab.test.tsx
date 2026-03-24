import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ScenariosTab } from './ScenariosTab'
import { ALL_STRESS_RESULTS } from '../test-utils/stressMocks'

vi.mock('../api/scenarios', () => ({
  listScenarios: vi.fn().mockResolvedValue([]),
  listApprovedScenarios: vi.fn().mockResolvedValue([]),
  createScenario: vi.fn(),
  submitScenario: vi.fn(),
  approveScenario: vi.fn(),
  retireScenario: vi.fn(),
}))

const defaultProps = {
  bookId: 'book-1',
  results: [],
  loading: false,
  error: null,
  selectedScenario: null,
  onSelectScenario: vi.fn(),
  confidenceLevel: 'CL_95',
  onConfidenceLevelChange: vi.fn(),
  timeHorizonDays: '1',
  onTimeHorizonDaysChange: vi.fn(),
  onRunAll: vi.fn(),
  onAppendResult: vi.fn(),
}

describe('ScenariosTab', () => {
  it('renders the scenarios tab with control bar and empty state', () => {
    render(<ScenariosTab {...defaultProps} />)

    expect(screen.getByTestId('scenarios-tab')).toBeInTheDocument()
    expect(screen.getByTestId('scenario-control-bar')).toBeInTheDocument()
    expect(screen.getByTestId('no-results')).toBeInTheDocument()
  })

  it('renders comparison table when results are provided', () => {
    render(<ScenariosTab {...defaultProps} results={ALL_STRESS_RESULTS} />)

    expect(screen.getByTestId('scenario-comparison-table')).toBeInTheDocument()
    expect(screen.getAllByTestId('scenario-row')).toHaveLength(4)
  })

  it('shows loading state', () => {
    render(<ScenariosTab {...defaultProps} loading={true} />)

    expect(screen.getByTestId('stress-loading')).toBeInTheDocument()
  })

  it('shows error message', () => {
    render(<ScenariosTab {...defaultProps} error="Something went wrong" />)

    expect(screen.getByTestId('stress-error')).toHaveTextContent('Something went wrong')
  })

  it('shows scenario library grid when Manage Scenarios is clicked', async () => {
    render(<ScenariosTab {...defaultProps} />)

    await userEvent.click(screen.getByTestId('manage-scenarios-btn'))
    expect(screen.getByTestId('scenario-library-grid')).toBeInTheDocument()
  })
})
