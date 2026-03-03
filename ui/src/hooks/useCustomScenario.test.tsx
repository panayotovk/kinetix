import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useCustomScenario } from './useCustomScenario'

const ASSET_CLASSES = ['EQUITY', 'FIXED_INCOME', 'COMMODITY', 'FX', 'DERIVATIVE']

describe('useCustomScenario', () => {
  const mockOnSave = vi.fn()
  const mockOnRunAdHoc = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should initialize with default shock multipliers of 1.0 for all asset classes', () => {
    const { result } = renderHook(() =>
      useCustomScenario({ onSave: mockOnSave, onRunAdHoc: mockOnRunAdHoc }),
    )

    for (const ac of ASSET_CLASSES) {
      expect(result.current.volShocks[ac]).toBe(1.0)
      expect(result.current.priceShocks[ac]).toBe(1.0)
    }
  })

  it('should update vol shock for a specific asset class', () => {
    const { result } = renderHook(() =>
      useCustomScenario({ onSave: mockOnSave, onRunAdHoc: mockOnRunAdHoc }),
    )

    act(() => {
      result.current.setVolShock('EQUITY', 2.5)
    })

    expect(result.current.volShocks.EQUITY).toBe(2.5)
    expect(result.current.volShocks.COMMODITY).toBe(1.0)
  })

  it('should validate vol shock range 0.5 to 5.0', () => {
    const { result } = renderHook(() =>
      useCustomScenario({ onSave: mockOnSave, onRunAdHoc: mockOnRunAdHoc }),
    )

    act(() => {
      result.current.setVolShock('EQUITY', 0.3)
    })

    expect(result.current.errors.EQUITY).toContain('Vol shock must be between 0.5 and 5.0')
  })

  it('should validate price shock range 0.5 to 1.5', () => {
    const { result } = renderHook(() =>
      useCustomScenario({ onSave: mockOnSave, onRunAdHoc: mockOnRunAdHoc }),
    )

    act(() => {
      result.current.setPriceShock('EQUITY', 2.0)
    })

    expect(result.current.errors.EQUITY).toContain('Price shock must be between 0.5 and 1.5')
  })

  it('should prevent save when name is empty', () => {
    const { result } = renderHook(() =>
      useCustomScenario({ onSave: mockOnSave, onRunAdHoc: mockOnRunAdHoc }),
    )

    act(() => {
      result.current.save()
    })

    expect(mockOnSave).not.toHaveBeenCalled()
    expect(result.current.nameError).toBe('Scenario name is required')
  })

  it('should call save callback when save is triggered with valid data', () => {
    const { result } = renderHook(() =>
      useCustomScenario({ onSave: mockOnSave, onRunAdHoc: mockOnRunAdHoc }),
    )

    act(() => {
      result.current.setName('My Scenario')
      result.current.setDescription('Test description')
    })

    act(() => {
      result.current.save()
    })

    expect(mockOnSave).toHaveBeenCalledWith({
      name: 'My Scenario',
      description: 'Test description',
      volShocks: { EQUITY: 1.0, FIXED_INCOME: 1.0, COMMODITY: 1.0, FX: 1.0, DERIVATIVE: 1.0 },
      priceShocks: { EQUITY: 1.0, FIXED_INCOME: 1.0, COMMODITY: 1.0, FX: 1.0, DERIVATIVE: 1.0 },
    })
  })

  it('should call run callback when run ad-hoc is triggered', () => {
    const { result } = renderHook(() =>
      useCustomScenario({ onSave: mockOnSave, onRunAdHoc: mockOnRunAdHoc }),
    )

    act(() => {
      result.current.setVolShock('EQUITY', 3.0)
      result.current.setPriceShock('EQUITY', 0.6)
    })

    act(() => {
      result.current.runAdHoc()
    })

    expect(mockOnRunAdHoc).toHaveBeenCalledWith({
      volShocks: { EQUITY: 3.0, FIXED_INCOME: 1.0, COMMODITY: 1.0, FX: 1.0, DERIVATIVE: 1.0 },
      priceShocks: { EQUITY: 0.6, FIXED_INCOME: 1.0, COMMODITY: 1.0, FX: 1.0, DERIVATIVE: 1.0 },
    })
  })

  it('should reset form when reset is called', () => {
    const { result } = renderHook(() =>
      useCustomScenario({ onSave: mockOnSave, onRunAdHoc: mockOnRunAdHoc }),
    )

    act(() => {
      result.current.setName('Test')
      result.current.setDescription('Desc')
      result.current.setVolShock('EQUITY', 3.0)
    })

    act(() => {
      result.current.reset()
    })

    expect(result.current.name).toBe('')
    expect(result.current.description).toBe('')
    expect(result.current.volShocks.EQUITY).toBe(1.0)
  })
})
