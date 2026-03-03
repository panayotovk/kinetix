import { useState, useEffect, useCallback } from 'react'
import type { StressScenarioDto } from '../types'
import { listScenarios, submitScenario, approveScenario, retireScenario } from '../api/scenarios'

export function useScenarioGovernance() {
  const [scenarios, setScenarios] = useState<StressScenarioDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await listScenarios()
      setScenarios(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load scenarios')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  const submit = useCallback(async (id: string) => {
    await submitScenario(id)
    await refresh()
  }, [refresh])

  const approve = useCallback(async (id: string) => {
    await approveScenario(id, 'user')
    await refresh()
  }, [refresh])

  const retire = useCallback(async (id: string) => {
    await retireScenario(id)
    await refresh()
  }, [refresh])

  return { scenarios, loading, error, submit, approve, retire, refresh }
}
