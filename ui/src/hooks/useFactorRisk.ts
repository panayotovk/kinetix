import { useCallback, useEffect, useRef, useState } from 'react'
import type { FactorRiskDto } from '../types'
import { fetchLatestFactorRisk } from '../api/factorRisk'

interface UseFactorRiskResult {
  result: FactorRiskDto | null
  loading: boolean
  error: string | null
}

export function useFactorRisk(bookId: string | null): UseFactorRiskResult {
  const [result, setResult] = useState<FactorRiskDto | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!bookId) return

    setLoading(true)
    setError(null)

    try {
      const data = await fetchLatestFactorRisk(bookId)
      setResult(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load factor risk')
    } finally {
      setLoading(false)
    }
  }, [bookId])

  const loadRef = useRef(load)
  loadRef.current = load

  useEffect(() => {
    if (!bookId) {
      setResult(null)
      return
    }
    loadRef.current()
  }, [bookId])

  return { result, loading, error }
}
