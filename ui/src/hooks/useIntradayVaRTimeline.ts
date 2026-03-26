import { useEffect, useRef, useState } from 'react'
import { fetchIntradayVaRTimeline } from '../api/intradayVaRTimeline'
import type { IntradayVaRPointDto, TradeAnnotationDto } from '../types'

const POLL_INTERVAL_MS = 60_000

interface UseIntradayVaRTimelineResult {
  varPoints: IntradayVaRPointDto[]
  tradeAnnotations: TradeAnnotationDto[]
  loading: boolean
  error: string | null
}

export function useIntradayVaRTimeline(
  bookId: string | null,
  from: string,
  to: string,
): UseIntradayVaRTimelineResult {
  const [varPoints, setVarPoints] = useState<IntradayVaRPointDto[]>([])
  const [tradeAnnotations, setTradeAnnotations] = useState<TradeAnnotationDto[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const abortRef = useRef<AbortController | null>(null)

  const loadKey = bookId ? `${bookId}|${from}|${to}` : ''
  const [prevLoadKey, setPrevLoadKey] = useState('')

  if (loadKey !== prevLoadKey) {
    setPrevLoadKey(loadKey)
    if (loadKey) {
      setLoading(true)
      setError(null)
    } else {
      setVarPoints([])
      setTradeAnnotations([])
      setLoading(false)
      setError(null)
    }
  }

  useEffect(() => {
    if (!bookId) return

    function fetch() {
      abortRef.current?.abort()
      const controller = new AbortController()
      abortRef.current = controller

      fetchIntradayVaRTimeline(bookId!, from, to)
        .then((data) => {
          if (controller.signal.aborted) return
          setVarPoints((prev) => {
            if (prev.length === data.varPoints.length && prev.length > 0 && prev[prev.length - 1].timestamp === data.varPoints[data.varPoints.length - 1].timestamp && prev[0].timestamp === data.varPoints[0].timestamp) return prev
            return data.varPoints
          })
          setTradeAnnotations((prev) => {
            if (prev.length === data.tradeAnnotations.length && JSON.stringify(prev) === JSON.stringify(data.tradeAnnotations)) return prev
            return data.tradeAnnotations
          })
        })
        .catch((err: unknown) => {
          if (controller.signal.aborted) return
          setError(err instanceof Error ? err.message : String(err))
          setVarPoints([])
          setTradeAnnotations([])
        })
        .finally(() => {
          if (!controller.signal.aborted) setLoading(false)
        })
    }

    fetch()
    const interval = setInterval(fetch, POLL_INTERVAL_MS)

    return () => {
      clearInterval(interval)
      abortRef.current?.abort()
    }
  }, [bookId, from, to])

  return { varPoints, tradeAnnotations, loading, error }
}
