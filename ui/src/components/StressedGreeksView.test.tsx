import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StressedGreeksView } from './StressedGreeksView'
import type { StressedGreeksDto } from '../types'

const greeksData: StressedGreeksDto = {
  baseDelta: '15000.00',
  stressedDelta: '22000.00',
  baseGamma: '500.00',
  stressedGamma: '-200.00',
  baseVega: '8000.00',
  stressedVega: '12000.00',
  baseTheta: '-1200.00',
  stressedTheta: '-2500.00',
  baseRho: '3000.00',
  stressedRho: '4500.00',
}

describe('StressedGreeksView', () => {
  it('should render base vs stressed delta, gamma, vega, theta, rho', () => {
    render(<StressedGreeksView greeks={greeksData} />)

    expect(screen.getByText('Delta')).toBeInTheDocument()
    expect(screen.getByText('Gamma')).toBeInTheDocument()
    expect(screen.getByText('Vega')).toBeInTheDocument()
    expect(screen.getByText('Theta')).toBeInTheDocument()
    expect(screen.getByText('Rho')).toBeInTheDocument()

    // Check that values are rendered
    expect(screen.getByTestId('greeks-view')).toBeInTheDocument()
  })

  it('should highlight sign changes with warning color', () => {
    // Gamma flips from positive 500 to negative -200
    render(<StressedGreeksView greeks={greeksData} />)

    const gammaRow = screen.getByTestId('greek-row-Gamma')
    expect(gammaRow).toHaveTextContent('SIGN FLIP')
  })

  it('should show percentage change', () => {
    render(<StressedGreeksView greeks={greeksData} />)

    // Delta: (22000 - 15000) / 15000 = +46.7%
    const deltaRow = screen.getByTestId('greek-row-Delta')
    expect(deltaRow).toHaveTextContent('+46.7%')
  })

  it('should show empty state when Greeks data unavailable', () => {
    render(<StressedGreeksView greeks={undefined} />)

    expect(screen.getByText(/No stressed Greeks data/)).toBeInTheDocument()
  })
})
