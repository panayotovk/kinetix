import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchLimits, type LimitDefinitionDto } from '../api/limits'

export interface UseLimitsResult {
  limits: LimitDefinitionDto[]
  loading: boolean
  error: string | null
  refresh: () => void
}

export function useLimits(): UseLimitsResult {
  const [limits, setLimits] = useState<LimitDefinitionDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [refreshSignal, setRefreshSignal] = useState(0)

  const refresh = useCallback(() => {
    setRefreshSignal((prev) => prev + 1)
  }, [])

  const abortRef = useRef<AbortController | null>(null)

  // Reset loading/error during render when refreshSignal changes
  // (React-supported "set state during render" pattern, mirroring useEodTimeline).
  const [prevSignal, setPrevSignal] = useState(-1)
  if (refreshSignal !== prevSignal) {
    setPrevSignal(refreshSignal)
    setLoading(true)
    setError(null)
  }

  useEffect(() => {
    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller

    fetchLimits()
      .then((data) => {
        if (controller.signal.aborted) return
        setLimits(data)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setError(err instanceof Error ? err.message : String(err))
        setLimits([])
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })

    return () => {
      controller.abort()
    }
  }, [refreshSignal])

  return { limits, loading, error, refresh }
}
