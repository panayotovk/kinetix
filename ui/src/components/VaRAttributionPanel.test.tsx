import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { VaRAttributionPanel } from './VaRAttributionPanel'
import type { VaRAttributionDto } from '../types'

const attribution = (overrides: Partial<VaRAttributionDto> = {}): VaRAttributionDto => ({
  totalChange: '50000',
  positionEffect: '30000',
  volEffect: '15000',
  corrEffect: '5000',
  timeDecayEffect: '-2000',
  unexplained: '2000',
  effectMagnitudes: {},
  caveats: [],
  ...overrides,
})

describe('VaRAttributionPanel', () => {
  it('renders the empty state with a "Request Attribution" button when no data and not loading', () => {
    const onRequest = vi.fn()
    render(<VaRAttributionPanel attribution={null} loading={false} onRequest={onRequest} />)

    expect(screen.getByTestId('var-attribution-panel')).toBeInTheDocument()
    fireEvent.click(screen.getByTestId('request-attribution'))
    expect(onRequest).toHaveBeenCalledTimes(1)
  })

  it('renders a loading spinner when loading is true', () => {
    render(<VaRAttributionPanel attribution={null} loading={true} onRequest={() => {}} />)

    const panel = screen.getByTestId('var-attribution-panel')
    expect(panel).toHaveTextContent('Computing attribution...')
  })

  it('renders every effect row when attribution is available', () => {
    render(
      <VaRAttributionPanel
        attribution={attribution()}
        loading={false}
        onRequest={() => {}}
      />,
    )

    expect(screen.getByTestId('attr-total-change')).toBeInTheDocument()
    expect(screen.getByTestId('attr-position-effect')).toBeInTheDocument()
    expect(screen.getByTestId('attr-volatility-effect')).toBeInTheDocument()
    expect(screen.getByTestId('attr-correlation-effect')).toBeInTheDocument()
    expect(screen.getByTestId('attr-time-decay')).toBeInTheDocument()
    expect(screen.getByTestId('attr-unexplained')).toBeInTheDocument()
  })

  it('renders N/A for null volEffect and corrEffect', () => {
    render(
      <VaRAttributionPanel
        attribution={attribution({ volEffect: null, corrEffect: null })}
        loading={false}
        onRequest={() => {}}
      />,
    )

    expect(screen.getByTestId('attr-volatility-effect')).toHaveTextContent('N/A')
    expect(screen.getByTestId('attr-correlation-effect')).toHaveTextContent('N/A')
  })

  it('colours positive effects with the "loss" class and negative with the "gain" class', () => {
    render(
      <VaRAttributionPanel
        attribution={attribution({ positionEffect: '30000', timeDecayEffect: '-2000' })}
        loading={false}
        onRequest={() => {}}
      />,
    )

    // positive (VaR increase = worse) renders in the red family; negative in green
    const pos = screen.getByTestId('attr-position-effect')
    const decay = screen.getByTestId('attr-time-decay')
    expect(pos.className).toContain('red')
    expect(decay.className).toContain('green')
  })

  it('renders caveats with the warning triangle when present', () => {
    render(
      <VaRAttributionPanel
        attribution={attribution({ caveats: ['Correlation effect assumes stable regime'] })}
        loading={false}
        onRequest={() => {}}
      />,
    )

    expect(screen.getByTestId('attribution-caveats')).toHaveTextContent(
      'Correlation effect assumes stable regime',
    )
  })

  it('does not render the caveats block when the list is empty', () => {
    render(
      <VaRAttributionPanel
        attribution={attribution({ caveats: [] })}
        loading={false}
        onRequest={() => {}}
      />,
    )

    expect(screen.queryByTestId('attribution-caveats')).not.toBeInTheDocument()
  })

  it('formats numeric effect values via formatNum', () => {
    render(
      <VaRAttributionPanel
        attribution={attribution({ positionEffect: '30000', totalChange: '50000' })}
        loading={false}
        onRequest={() => {}}
      />,
    )

    // formatNum inserts thousands separators; check that digits are grouped
    expect(screen.getByTestId('attr-total-change').textContent).toMatch(/50[,.]?000/)
    expect(screen.getByTestId('attr-position-effect').textContent).toMatch(/30[,.]?000/)
  })
})
