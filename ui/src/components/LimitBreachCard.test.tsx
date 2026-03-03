import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { LimitBreachCard } from './LimitBreachCard'
import { makeLimitBreach } from '../test-utils/stressMocks'

describe('LimitBreachCard', () => {
  it('should render green badge when utilization is below 80%', () => {
    const breaches = [makeLimitBreach({ limitValue: '1000000.00', stressedValue: '500000.00', breachSeverity: 'OK' })]
    render(<LimitBreachCard breaches={breaches} />)

    expect(screen.getByTestId('severity-badge-0')).toHaveTextContent('OK')
    expect(screen.getByTestId('severity-badge-0').className).toContain('bg-green-100')
  })

  it('should render amber badge when utilization is between 80% and 100%', () => {
    const breaches = [makeLimitBreach({ limitValue: '1000000.00', stressedValue: '900000.00', breachSeverity: 'WARNING' })]
    render(<LimitBreachCard breaches={breaches} />)

    expect(screen.getByTestId('severity-badge-0')).toHaveTextContent('WARNING')
    expect(screen.getByTestId('severity-badge-0').className).toContain('bg-yellow-100')
  })

  it('should render red badge when utilization exceeds 100%', () => {
    const breaches = [makeLimitBreach({ limitValue: '1000000.00', stressedValue: '1200000.00', breachSeverity: 'BREACHED' })]
    render(<LimitBreachCard breaches={breaches} />)

    expect(screen.getByTestId('severity-badge-0')).toHaveTextContent('BREACHED')
    expect(screen.getByTestId('severity-badge-0').className).toContain('bg-red-100')
  })

  it('should display utilization progress bar with correct width and color', () => {
    const breaches = [makeLimitBreach({ limitValue: '1000000.00', stressedValue: '900000.00', breachSeverity: 'WARNING' })]
    render(<LimitBreachCard breaches={breaches} />)

    const bar = screen.getByTestId('utilization-bar-0')
    expect(bar.style.width).toBe('90%')
    expect(bar.className).toContain('bg-amber')
  })

  it('should cap utilization bar at 100% width for breached limits', () => {
    const breaches = [makeLimitBreach({ limitValue: '1000000.00', stressedValue: '1500000.00', breachSeverity: 'BREACHED' })]
    render(<LimitBreachCard breaches={breaches} />)

    const bar = screen.getByTestId('utilization-bar-0')
    expect(bar.style.width).toBe('100%')
  })

  it('should show no breaches message when all limits are within bounds', () => {
    render(<LimitBreachCard breaches={[]} />)

    expect(screen.getByTestId('no-breaches')).toHaveTextContent('No limit breaches')
  })

  it('should associate utilization bar with aria-label describing percentage', () => {
    const breaches = [makeLimitBreach({ limitValue: '1000000.00', stressedValue: '900000.00' })]
    render(<LimitBreachCard breaches={breaches} />)

    const bar = screen.getByTestId('utilization-bar-0')
    expect(bar.parentElement).toHaveAttribute('aria-label', expect.stringContaining('90%'))
  })

  it('should display limit type and level for each breach', () => {
    const breaches = [
      makeLimitBreach({ limitType: 'NOTIONAL', limitLevel: 'FIRM', breachSeverity: 'BREACHED' }),
      makeLimitBreach({ limitType: 'VAR', limitLevel: 'DESK', breachSeverity: 'WARNING' }),
    ]
    render(<LimitBreachCard breaches={breaches} />)

    const rows = screen.getAllByTestId('breach-row')
    expect(rows[0]).toHaveTextContent('NOTIONAL')
    expect(rows[0]).toHaveTextContent('FIRM')
    expect(rows[1]).toHaveTextContent('VAR')
    expect(rows[1]).toHaveTextContent('DESK')
  })
})
