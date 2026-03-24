import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HistoricalReplayPanel } from './HistoricalReplayPanel'
import type { HistoricalReplayResultDto } from '../types'

const mockResult: HistoricalReplayResultDto = {
  scenarioName: 'GFC_2008',
  totalPnlImpact: '-125000.00',
  positionImpacts: [
    {
      instrumentId: 'AAPL',
      assetClass: 'EQUITY',
      marketValue: '15500.00',
      pnlImpact: '-125000.00',
      dailyPnl: ['-50000.00', '-25000.00', '10000.00', '-40000.00', '-20000.00'],
      proxyUsed: false,
    },
    {
      instrumentId: 'GOOGL',
      assetClass: 'EQUITY',
      marketValue: '142500.00',
      pnlImpact: '-85000.00',
      dailyPnl: ['-30000.00', '-15000.00', '5000.00', '-25000.00', '-20000.00'],
      proxyUsed: true,
    },
  ],
  windowStart: '2008-09-15',
  windowEnd: '2008-09-19',
  calculatedAt: '2025-01-15T10:00:00Z',
}

describe('HistoricalReplayPanel', () => {
  it('renders the panel header', () => {
    render(
      <HistoricalReplayPanel
        scenarios={['GFC_2008', 'COVID_2020']}
        result={null}
        loading={false}
        error={null}
        selectedScenario="GFC_2008"
        onScenarioChange={() => {}}
        onRun={() => {}}
        bookId="port-1"
      />,
    )
    expect(screen.getByTestId('historical-replay-panel')).toBeDefined()
  })

  it('renders scenario dropdown with available scenarios', () => {
    render(
      <HistoricalReplayPanel
        scenarios={['GFC_2008', 'COVID_2020', 'TAPER_TANTRUM_2013']}
        result={null}
        loading={false}
        error={null}
        selectedScenario="GFC_2008"
        onScenarioChange={() => {}}
        onRun={() => {}}
        bookId="port-1"
      />,
    )
    const dropdown = screen.getByTestId('replay-scenario-dropdown')
    expect(dropdown).toBeDefined()
  })

  it('shows loading indicator when loading', () => {
    render(
      <HistoricalReplayPanel
        scenarios={['GFC_2008']}
        result={null}
        loading={true}
        error={null}
        selectedScenario="GFC_2008"
        onScenarioChange={() => {}}
        onRun={() => {}}
        bookId="port-1"
      />,
    )
    expect(screen.getByTestId('replay-loading')).toBeDefined()
  })

  it('shows error message when error is present', () => {
    render(
      <HistoricalReplayPanel
        scenarios={['GFC_2008']}
        result={null}
        loading={false}
        error="Failed to run replay"
        selectedScenario="GFC_2008"
        onScenarioChange={() => {}}
        onRun={() => {}}
        bookId="port-1"
      />,
    )
    expect(screen.getByTestId('replay-error')).toBeDefined()
    expect(screen.getByText('Failed to run replay')).toBeDefined()
  })

  it('displays total P&L impact when result is available', () => {
    render(
      <HistoricalReplayPanel
        scenarios={['GFC_2008']}
        result={mockResult}
        loading={false}
        error={null}
        selectedScenario="GFC_2008"
        onScenarioChange={() => {}}
        onRun={() => {}}
        bookId="port-1"
      />,
    )
    expect(screen.getByTestId('replay-total-pnl')).toBeDefined()
  })

  it('displays position impacts table when result is available', () => {
    render(
      <HistoricalReplayPanel
        scenarios={['GFC_2008']}
        result={mockResult}
        loading={false}
        error={null}
        selectedScenario="GFC_2008"
        onScenarioChange={() => {}}
        onRun={() => {}}
        bookId="port-1"
      />,
    )
    expect(screen.getByTestId('replay-position-impacts')).toBeDefined()
    expect(screen.getByText('AAPL')).toBeDefined()
    expect(screen.getByText('GOOGL')).toBeDefined()
  })

  it('shows proxy badge for proxy positions', () => {
    render(
      <HistoricalReplayPanel
        scenarios={['GFC_2008']}
        result={mockResult}
        loading={false}
        error={null}
        selectedScenario="GFC_2008"
        onScenarioChange={() => {}}
        onRun={() => {}}
        bookId="port-1"
      />,
    )
    expect(screen.getByTestId('proxy-badge-GOOGL')).toBeDefined()
  })

  it('calls onRun when Run button is clicked', async () => {
    const onRun = vi.fn()
    render(
      <HistoricalReplayPanel
        scenarios={['GFC_2008']}
        result={null}
        loading={false}
        error={null}
        selectedScenario="GFC_2008"
        onScenarioChange={() => {}}
        onRun={onRun}
        bookId="port-1"
      />,
    )
    await userEvent.click(screen.getByTestId('replay-run-btn'))
    expect(onRun).toHaveBeenCalledOnce()
  })

  it('shows window dates when available', () => {
    render(
      <HistoricalReplayPanel
        scenarios={['GFC_2008']}
        result={mockResult}
        loading={false}
        error={null}
        selectedScenario="GFC_2008"
        onScenarioChange={() => {}}
        onRun={() => {}}
        bookId="port-1"
      />,
    )
    expect(screen.getByText(/2008-09-15/)).toBeDefined()
    expect(screen.getByText(/2008-09-19/)).toBeDefined()
  })
})
