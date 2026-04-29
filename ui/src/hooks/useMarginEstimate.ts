import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchMarginEstimate, type MarginEstimateDto } from '../api/margin'

export interface UseMarginEstimateResult {
  estimate: MarginEstimateDto | null
  loading: boolean
  error: string | null
  refresh: () => void
}

export function useMarginEstimate(bookId: string | null): UseMarginEstimateResult {
  const [estimate, setEstimate] = useState<MarginEstimateDto | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [refreshSignal, setRefreshSignal] = useState(0)

  const refresh = useCallback(() => {
    setRefreshSignal((prev) => prev + 1)
  }, [])

  const abortRef = useRef<AbortController | null>(null)

  const loadKey = bookId ? `${bookId}|${refreshSignal}` : ''
  const [prevLoadKey, setPrevLoadKey] = useState('')

  if (loadKey !== prevLoadKey) {
    setPrevLoadKey(loadKey)
    if (loadKey) {
      setLoading(true)
      setError(null)
    } else {
      setEstimate(null)
      setLoading(false)
      setError(null)
    }
  }

  useEffect(() => {
    if (!bookId) return

    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller

    fetchMarginEstimate(bookId)
      .then((data) => {
        if (controller.signal.aborted) return
        setEstimate(data)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setError(err instanceof Error ? err.message : String(err))
        setEstimate(null)
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })

    return () => {
      controller.abort()
    }
  }, [bookId, refreshSignal])

  return { estimate, loading, error, refresh }
}
