import { useEffect, useState } from 'react'
import { fetchKrd } from '../api/krd'
import type { KrdBucketDto, InstrumentKrdResultDto } from '../types'

interface UseKrdResult {
  aggregated: KrdBucketDto[]
  instruments: InstrumentKrdResultDto[]
  loading: boolean
  error: string | null
}

export function useKrd(bookId: string | null): UseKrdResult {
  const [aggregated, setAggregated] = useState<KrdBucketDto[]>([])
  const [instruments, setInstruments] = useState<InstrumentKrdResultDto[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [prevBookId, setPrevBookId] = useState<string | null>(null)

  if (bookId !== prevBookId) {
    setPrevBookId(bookId)
    if (!bookId) {
      setAggregated([])
      setInstruments([])
      setLoading(false)
      setError(null)
    } else {
      setLoading(true)
      setError(null)
    }
  }

  useEffect(() => {
    if (!bookId) return

    let cancelled = false

    fetchKrd(bookId)
      .then((data) => {
        if (cancelled) return
        setAggregated(data.aggregated)
        setInstruments(data.instruments)
      })
      .catch((err: unknown) => {
        if (cancelled) return
        setError(err instanceof Error ? err.message : String(err))
        setAggregated([])
        setInstruments([])
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => { cancelled = true }
  }, [bookId])

  return { aggregated, instruments, loading, error }
}
