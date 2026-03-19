import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { CorrelationHeatmap } from './CorrelationHeatmap'

describe('CorrelationHeatmap', () => {
  it('renders heatmap with all 5 asset classes by default', () => {
    render(<CorrelationHeatmap />)

    expect(screen.getByTestId('correlation-heatmap')).toBeInTheDocument()

    // 5x5 = 25 cells
    const allClasses = ['EQUITY', 'FIXED_INCOME', 'FX', 'COMMODITY', 'DERIVATIVE']
    for (const row of allClasses) {
      for (const col of allClasses) {
        expect(screen.getByTestId(`correlation-cell-${row}-${col}`)).toBeInTheDocument()
      }
    }
  })

  it('filters to provided asset classes', () => {
    render(<CorrelationHeatmap assetClasses={['EQUITY', 'FX']} />)

    // Should have 2x2 = 4 cells
    expect(screen.getByTestId('correlation-cell-EQUITY-EQUITY')).toBeInTheDocument()
    expect(screen.getByTestId('correlation-cell-EQUITY-FX')).toBeInTheDocument()
    expect(screen.getByTestId('correlation-cell-FX-EQUITY')).toBeInTheDocument()
    expect(screen.getByTestId('correlation-cell-FX-FX')).toBeInTheDocument()

    // Should NOT have cells for other asset classes
    expect(screen.queryByTestId('correlation-cell-EQUITY-FIXED_INCOME')).not.toBeInTheDocument()
    expect(screen.queryByTestId('correlation-cell-COMMODITY-COMMODITY')).not.toBeInTheDocument()
  })

  it('displays correct correlation value in cells', () => {
    render(<CorrelationHeatmap />)

    const equityFxCell = screen.getByTestId('correlation-cell-EQUITY-FX')
    expect(equityFxCell).toHaveTextContent('0.30')

    const fiEquityCell = screen.getByTestId('correlation-cell-FIXED_INCOME-EQUITY')
    expect(fiEquityCell).toHaveTextContent('-0.20')
  })

  it('diagonal cells show 1.00', () => {
    render(<CorrelationHeatmap />)

    expect(screen.getByTestId('correlation-cell-EQUITY-EQUITY')).toHaveTextContent('1.00')
    expect(screen.getByTestId('correlation-cell-FX-FX')).toHaveTextContent('1.00')
    expect(screen.getByTestId('correlation-cell-DERIVATIVE-DERIVATIVE')).toHaveTextContent('1.00')
  })
})
