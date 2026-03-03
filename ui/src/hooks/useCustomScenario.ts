import { useCallback, useState } from 'react'

const ASSET_CLASSES = ['EQUITY', 'FIXED_INCOME', 'COMMODITY', 'FX', 'DERIVATIVE'] as const

function defaultShocks(): Record<string, number> {
  return Object.fromEntries(ASSET_CLASSES.map((ac) => [ac, 1.0]))
}

export interface ScenarioSavePayload {
  name: string
  description: string
  volShocks: Record<string, number>
  priceShocks: Record<string, number>
}

export interface ScenarioRunPayload {
  volShocks: Record<string, number>
  priceShocks: Record<string, number>
}

interface UseCustomScenarioOptions {
  onSave: (payload: ScenarioSavePayload) => void
  onRunAdHoc: (payload: ScenarioRunPayload) => void
}

export function useCustomScenario({ onSave, onRunAdHoc }: UseCustomScenarioOptions) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [volShocks, setVolShocks] = useState<Record<string, number>>(defaultShocks)
  const [priceShocks, setPriceShocks] = useState<Record<string, number>>(defaultShocks)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [nameError, setNameError] = useState<string | null>(null)

  const setVolShock = useCallback((assetClass: string, value: number) => {
    setVolShocks((prev) => ({ ...prev, [assetClass]: value }))
    setErrors((prev) => {
      const next = { ...prev }
      if (value < 0.5 || value > 5.0) {
        next[assetClass] = 'Vol shock must be between 0.5 and 5.0'
      } else {
        delete next[assetClass]
      }
      return next
    })
  }, [])

  const setPriceShock = useCallback((assetClass: string, value: number) => {
    setPriceShocks((prev) => ({ ...prev, [assetClass]: value }))
    setErrors((prev) => {
      const next = { ...prev }
      if (value < 0.5 || value > 1.5) {
        next[assetClass] = 'Price shock must be between 0.5 and 1.5'
      } else {
        delete next[assetClass]
      }
      return next
    })
  }, [])

  const hasValidationErrors = Object.keys(errors).length > 0

  const save = useCallback(() => {
    if (!name.trim()) {
      setNameError('Scenario name is required')
      return
    }
    setNameError(null)
    if (hasValidationErrors) return
    onSave({ name, description, volShocks, priceShocks })
  }, [name, description, volShocks, priceShocks, hasValidationErrors, onSave])

  const runAdHoc = useCallback(() => {
    if (hasValidationErrors) return
    onRunAdHoc({ volShocks, priceShocks })
  }, [volShocks, priceShocks, hasValidationErrors, onRunAdHoc])

  const reset = useCallback(() => {
    setName('')
    setDescription('')
    setVolShocks(defaultShocks())
    setPriceShocks(defaultShocks())
    setErrors({})
    setNameError(null)
  }, [])

  return {
    name,
    setName,
    description,
    setDescription,
    volShocks,
    priceShocks,
    setVolShock,
    setPriceShock,
    errors,
    nameError,
    save,
    runAdHoc,
    reset,
  }
}
