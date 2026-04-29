import { render, screen, fireEvent, within } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { LimitsPanel } from './LimitsPanel'
import type { LimitDefinitionDto } from '../api/limits'
import type { UseLimitsResult } from '../hooks/useLimits'

vi.mock('../hooks/useLimits', () => ({
  useLimits: vi.fn(),
}))

import { useLimits } from '../hooks/useLimits'
const mockUseLimits = useLimits as unknown as ReturnType<typeof vi.fn>

function limit(overrides: Partial<LimitDefinitionDto> = {}): LimitDefinitionDto {
  return {
    id: 'l-1',
    level: 'FIRM',
    entityId: 'firm-1',
    limitType: 'VAR',
    limitValue: '1000000',
    intradayLimit: null,
    overnightLimit: null,
    active: true,
    ...overrides,
  }
}

function mockHookState(overrides: Partial<UseLimitsResult> = {}): UseLimitsResult {
  return {
    limits: [],
    loading: false,
    error: null,
    refresh: vi.fn(),
    ...overrides,
  }
}

describe('LimitsPanel', () => {
  beforeEach(() => {
    mockUseLimits.mockReset()
  })

  it('renders a loading spinner while the hook reports loading', () => {
    mockUseLimits.mockReturnValue(mockHookState({ loading: true }))

    render(<LimitsPanel />)

    expect(screen.getByTestId('limits-loading')).toBeInTheDocument()
    expect(screen.queryByTestId('limits-panel')).not.toBeInTheDocument()
  })

  it('renders the no-limits empty state when the hook returns an empty array', () => {
    mockUseLimits.mockReturnValue(mockHookState({ limits: [] }))

    render(<LimitsPanel />)

    expect(screen.getByText('No limits defined')).toBeInTheDocument()
  })

  it('groups rows by hierarchy level in the FIRM → COUNTERPARTY order', () => {
    mockUseLimits.mockReturnValue(mockHookState({
      limits: [
        limit({ id: 'l-trader', level: 'TRADER', entityId: 'trader-a' }),
        limit({ id: 'l-firm', level: 'FIRM', entityId: 'firm-1' }),
        limit({ id: 'l-desk', level: 'DESK', entityId: 'desk-eq' }),
      ],
    }))

    render(<LimitsPanel />)

    expect(screen.getByTestId('limits-group-FIRM')).toBeInTheDocument()
    expect(screen.getByTestId('limits-group-DESK')).toBeInTheDocument()
    expect(screen.getByTestId('limits-group-TRADER')).toBeInTheDocument()

    // Confirm DOM order matches FIRM → DESK → TRADER
    const groups = screen.getAllByTestId(/^limits-group-/)
    expect(groups[0].getAttribute('data-testid')).toBe('limits-group-FIRM')
    expect(groups[1].getAttribute('data-testid')).toBe('limits-group-DESK')
    expect(groups[2].getAttribute('data-testid')).toBe('limits-group-TRADER')
  })

  it('renders limit rows with formatted numeric values and an active indicator', () => {
    mockUseLimits.mockReturnValue(mockHookState({
      limits: [
        limit({
          id: 'l-firm',
          level: 'FIRM',
          entityId: 'firm-1',
          limitValue: '1500000',
          intradayLimit: '1200000',
          overnightLimit: '1500000',
          active: true,
        }),
        limit({
          id: 'l-disabled',
          level: 'FIRM',
          entityId: 'firm-2',
          limitValue: '500000',
          active: false,
        }),
      ],
    }))

    render(<LimitsPanel />)

    const firmGroup = screen.getByTestId('limits-group-FIRM')
    const activeRow = within(firmGroup).getByTestId('limits-row-l-firm')
    expect(activeRow).toHaveTextContent('firm-1')
    expect(activeRow).toHaveTextContent('VAR')
    expect(activeRow).toHaveTextContent('1,500,000')
    expect(activeRow).toHaveTextContent('1,200,000')

    const disabledRow = within(firmGroup).getByTestId('limits-row-l-disabled')
    expect(disabledRow).toHaveTextContent('500,000')
  })

  it('filters rows when a single hierarchy level is selected from the dropdown', () => {
    mockUseLimits.mockReturnValue(mockHookState({
      limits: [
        limit({ id: 'l-firm', level: 'FIRM' }),
        limit({ id: 'l-desk', level: 'DESK' }),
      ],
    }))

    render(<LimitsPanel />)

    const filter = screen.getByTestId('limits-level-filter')
    fireEvent.change(filter, { target: { value: 'DESK' } })

    expect(screen.queryByTestId('limits-group-FIRM')).not.toBeInTheDocument()
    expect(screen.getByTestId('limits-group-DESK')).toBeInTheDocument()
  })

  it('shows the no-matching message when a level filter selects an empty bucket', () => {
    mockUseLimits.mockReturnValue(mockHookState({
      limits: [limit({ id: 'l-firm', level: 'FIRM' })],
    }))

    render(<LimitsPanel />)

    const filter = screen.getByTestId('limits-level-filter')
    fireEvent.change(filter, { target: { value: 'COUNTERPARTY' } })

    expect(screen.getByTestId('limits-no-matching')).toBeInTheDocument()
  })

  it('shows the error banner with retry when the hook reports an error', () => {
    const refresh = vi.fn()
    mockUseLimits.mockReturnValue(mockHookState({
      error: 'Failed to fetch limits: 500 Internal Server Error',
      refresh,
    }))

    render(<LimitsPanel />)

    expect(screen.getByTestId('limits-error')).toHaveTextContent('500 Internal Server Error')
    fireEvent.click(screen.getByTestId('limits-retry-btn'))
    expect(refresh).toHaveBeenCalledTimes(1)
  })
})
