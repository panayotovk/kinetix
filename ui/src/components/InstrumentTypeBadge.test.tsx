import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { InstrumentTypeBadge } from './InstrumentTypeBadge'
import { INSTRUMENT_TYPE_COLORS, formatInstrumentTypeLabel } from '../utils/instrumentTypes'

describe('InstrumentTypeBadge', () => {
  it('renders a badge with the formatted instrument type label', () => {
    render(<InstrumentTypeBadge instrumentType="CASH_EQUITY" />)

    expect(screen.getByText('Cash Equity')).toBeInTheDocument()
  })

  it('renders null when instrumentType is undefined', () => {
    const { container } = render(<InstrumentTypeBadge instrumentType={undefined} />)

    expect(container.firstChild).toBeNull()
  })

  it('renders null when instrumentType is empty string', () => {
    const { container } = render(<InstrumentTypeBadge instrumentType="" />)

    expect(container.firstChild).toBeNull()
  })

  it('applies a colored badge for CASH_EQUITY (blue)', () => {
    render(<InstrumentTypeBadge instrumentType="CASH_EQUITY" data-testid="badge" />)

    const badge = screen.getByTestId('badge')
    expect(badge.className).toContain('blue')
  })

  it('applies a colored badge for EQUITY_OPTION (amber)', () => {
    render(<InstrumentTypeBadge instrumentType="EQUITY_OPTION" data-testid="badge" />)

    const badge = screen.getByTestId('badge')
    expect(badge.className).toContain('amber')
  })

  it('applies a colored badge for GOVERNMENT_BOND (green)', () => {
    render(<InstrumentTypeBadge instrumentType="GOVERNMENT_BOND" data-testid="badge" />)

    const badge = screen.getByTestId('badge')
    expect(badge.className).toContain('green')
  })

  it('applies a colored badge for CORPORATE_BOND (green)', () => {
    render(<InstrumentTypeBadge instrumentType="CORPORATE_BOND" data-testid="badge" />)

    const badge = screen.getByTestId('badge')
    expect(badge.className).toContain('green')
  })

  it('applies a colored badge for FX_SPOT (purple)', () => {
    render(<InstrumentTypeBadge instrumentType="FX_SPOT" data-testid="badge" />)

    const badge = screen.getByTestId('badge')
    expect(badge.className).toContain('purple')
  })

  it('applies a colored badge for INTEREST_RATE_SWAP (teal)', () => {
    render(<InstrumentTypeBadge instrumentType="INTEREST_RATE_SWAP" data-testid="badge" />)

    const badge = screen.getByTestId('badge')
    expect(badge.className).toContain('teal')
  })

  it('applies a colored badge for FUTURES (orange)', () => {
    render(<InstrumentTypeBadge instrumentType="FUTURES" data-testid="badge" />)

    const badge = screen.getByTestId('badge')
    expect(badge.className).toContain('orange')
  })

  it('applies a colored badge for COMMODITY (brown / stone)', () => {
    render(<InstrumentTypeBadge instrumentType="COMMODITY" data-testid="badge" />)

    const badge = screen.getByTestId('badge')
    expect(badge.className).toContain('stone')
  })

  it('applies a fallback style for unknown instrument types', () => {
    render(<InstrumentTypeBadge instrumentType="UNKNOWN_TYPE" data-testid="badge" />)

    const badge = screen.getByTestId('badge')
    expect(badge).toBeInTheDocument()
  })

  it('formats underscore-separated type labels into title case', () => {
    expect(formatInstrumentTypeLabel('CASH_EQUITY')).toBe('Cash Equity')
    expect(formatInstrumentTypeLabel('EQUITY_OPTION')).toBe('Equity Option')
    expect(formatInstrumentTypeLabel('GOVERNMENT_BOND')).toBe('Government Bond')
    expect(formatInstrumentTypeLabel('INTEREST_RATE_SWAP')).toBe('Interest Rate Swap')
    expect(formatInstrumentTypeLabel('FX_SPOT')).toBe('FX Spot')
  })

  it('exposes INSTRUMENT_TYPE_COLORS map covering all 11 types', () => {
    const expectedTypes = [
      'CASH_EQUITY',
      'EQUITY_OPTION',
      'GOVERNMENT_BOND',
      'CORPORATE_BOND',
      'FX_SPOT',
      'FX_FORWARD',
      'INTEREST_RATE_SWAP',
      'FUTURES',
      'COMMODITY',
      'CREDIT_DEFAULT_SWAP',
      'CONVERTIBLE_BOND',
    ]

    expectedTypes.forEach((type) => {
      expect(INSTRUMENT_TYPE_COLORS).toHaveProperty(type)
    })
  })
})

describe('formatInstrumentTypeLabel', () => {
  it('keeps FX prefix uppercase', () => {
    expect(formatInstrumentTypeLabel('FX_SPOT')).toBe('FX Spot')
    expect(formatInstrumentTypeLabel('FX_FORWARD')).toBe('FX Forward')
  })

  it('formats single-word type names', () => {
    expect(formatInstrumentTypeLabel('FUTURES')).toBe('Futures')
    expect(formatInstrumentTypeLabel('COMMODITY')).toBe('Commodity')
  })
})
