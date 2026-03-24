import { useEffect, useRef, useState } from 'react'
import { fetchIntradayPnl } from '../api/intradayPnl'
import type { IntradayPnlSnapshotDto } from '../types'

interface UseIntradayPnlSeriesResult {
  snapshots: IntradayPnlSnapshotDto[]
  loading: boolean
  error: string | null
}

export function useIntradayPnlSeries(
  bookId: string | null,
  from: string,
  to: string,
): UseIntradayPnlSeriesResult {
  const [snapshots, setSnapshots] = useState<IntradayPnlSnapshotDto[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const abortRef = useRef<AbortController | null>(null)

  // "set state during render" pattern — avoids synchronous setState inside an effect
  const loadKey = bookId ? `${bookId}|${from}|${to}` : ''
  const [prevLoadKey, setPrevLoadKey] = useState('')

  if (loadKey !== prevLoadKey) {
    setPrevLoadKey(loadKey)
    if (loadKey) {
      setLoading(true)
      setError(null)
    } else {
      setSnapshots([])
      setLoading(false)
      setError(null)
    }
  }

  useEffect(() => {
    if (!bookId) return

    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller

    fetchIntradayPnl(bookId, from, to)
      .then((data) => {
        if (controller.signal.aborted) return
        setSnapshots(data.snapshots)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setError(err instanceof Error ? err.message : String(err))
        setSnapshots([])
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })

    return () => {
      controller.abort()
    }
  }, [bookId, from, to])

  return { snapshots, loading, error }
}
