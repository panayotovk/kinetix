import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchBrinsonAttribution } from '../api/benchmarkAttribution'
import type { BrinsonAttributionDto } from '../api/benchmarkAttribution'

export interface UseBrinsonAttributionResult {
  data: BrinsonAttributionDto | null
  loading: boolean
  error: string | null
}

export function useBrinsonAttribution(
  bookId: string | null,
  benchmarkId: string | null,
  asOfDate?: string,
): UseBrinsonAttributionResult {
  const [data, setData] = useState<BrinsonAttributionDto | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!bookId || !benchmarkId) return

    setLoading(true)
    setError(null)

    try {
      const result = await fetchBrinsonAttribution(bookId, benchmarkId, asOfDate)
      setData(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
      setData(null)
    } finally {
      setLoading(false)
    }
  }, [bookId, benchmarkId, asOfDate])

  const loadRef = useRef(load)
  loadRef.current = load

  useEffect(() => {
    if (!bookId || !benchmarkId) {
      setData(null)
      setLoading(false)
      setError(null)
      return
    }

    loadRef.current()
  }, [bookId, benchmarkId, asOfDate])

  return { data, loading, error }
}
