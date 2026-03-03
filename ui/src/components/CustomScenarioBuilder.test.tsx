import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { CustomScenarioBuilder } from './CustomScenarioBuilder'

const ASSET_CLASSES = ['EQUITY', 'FIXED_INCOME', 'COMMODITY', 'FX', 'DERIVATIVE']

describe('CustomScenarioBuilder', () => {
  const defaultProps = {
    open: true,
    onClose: vi.fn(),
    onSave: vi.fn(),
    onRunAdHoc: vi.fn(),
    saving: false,
    running: false,
  }

  it('should render scenario name input', () => {
    render(<CustomScenarioBuilder {...defaultProps} />)
    expect(screen.getByLabelText('Scenario name')).toBeInTheDocument()
  })

  it('should render vol shock input per asset class', () => {
    render(<CustomScenarioBuilder {...defaultProps} />)
    for (const ac of ASSET_CLASSES) {
      expect(screen.getByTestId(`vol-shock-${ac}`)).toBeInTheDocument()
    }
  })

  it('should render price shock input per asset class', () => {
    render(<CustomScenarioBuilder {...defaultProps} />)
    for (const ac of ASSET_CLASSES) {
      expect(screen.getByTestId(`price-shock-${ac}`)).toBeInTheDocument()
    }
  })

  it('should render Save and Run Ad-Hoc buttons', () => {
    render(<CustomScenarioBuilder {...defaultProps} />)
    expect(screen.getByTestId('scenario-save-btn')).toBeInTheDocument()
    expect(screen.getByTestId('scenario-run-btn')).toBeInTheDocument()
  })

  it('should show validation error when vol shock is below 0.5', () => {
    render(<CustomScenarioBuilder {...defaultProps} />)
    const input = screen.getByTestId('vol-shock-EQUITY')
    fireEvent.change(input, { target: { value: '0.3' } })
    expect(screen.getByText('Vol shock must be between 0.5 and 5.0')).toBeInTheDocument()
  })

  it('should show validation error when price shock is above 1.5', () => {
    render(<CustomScenarioBuilder {...defaultProps} />)
    const input = screen.getByTestId('price-shock-EQUITY')
    fireEvent.change(input, { target: { value: '2.0' } })
    expect(screen.getByText('Price shock must be between 0.5 and 1.5')).toBeInTheDocument()
  })

  it('should disable Save button when name is empty', () => {
    render(<CustomScenarioBuilder {...defaultProps} />)
    const saveBtn = screen.getByTestId('scenario-save-btn')
    expect(saveBtn).toBeDisabled()
  })

  it('should call onSave with scenario data when Save is clicked', () => {
    render(<CustomScenarioBuilder {...defaultProps} />)
    const nameInput = screen.getByLabelText('Scenario name')
    fireEvent.change(nameInput, { target: { value: 'My Scenario' } })
    const saveBtn = screen.getByTestId('scenario-save-btn')
    fireEvent.click(saveBtn)
    expect(defaultProps.onSave).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'My Scenario' }),
    )
  })

  it('should call onRunAdHoc with scenario data when Run Ad-Hoc is clicked', () => {
    render(<CustomScenarioBuilder {...defaultProps} />)
    const volInput = screen.getByTestId('vol-shock-EQUITY')
    fireEvent.change(volInput, { target: { value: '3.0' } })
    const runBtn = screen.getByTestId('scenario-run-btn')
    fireEvent.click(runBtn)
    expect(defaultProps.onRunAdHoc).toHaveBeenCalledWith(
      expect.objectContaining({
        volShocks: expect.objectContaining({ EQUITY: 3.0 }),
      }),
    )
  })

  it('should close on backdrop click', () => {
    render(<CustomScenarioBuilder {...defaultProps} />)
    const backdrop = screen.getByTestId('scenario-builder-backdrop')
    fireEvent.click(backdrop)
    expect(defaultProps.onClose).toHaveBeenCalled()
  })

  it('should close on Escape key', () => {
    render(<CustomScenarioBuilder {...defaultProps} />)
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(defaultProps.onClose).toHaveBeenCalled()
  })

  it('should have aria-labels on all inputs', () => {
    render(<CustomScenarioBuilder {...defaultProps} />)
    expect(screen.getByLabelText('Scenario name')).toBeInTheDocument()
    expect(screen.getByLabelText('Scenario description')).toBeInTheDocument()
    for (const ac of ASSET_CLASSES) {
      const label = ac.replace('_', ' ').toLowerCase()
      expect(screen.getByLabelText(`Vol shock for ${label}`)).toBeInTheDocument()
      expect(screen.getByLabelText(`Price shock for ${label}`)).toBeInTheDocument()
    }
  })
})
