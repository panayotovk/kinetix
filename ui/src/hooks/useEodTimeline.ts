import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchEodTimeline } from '../api/eodTimeline'
import type { EodTimelineEntryDto } from '../types'

function toDateString(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function defaultDateRange(): { from: string; to: string } {
  const today = new Date()
  // 30 business days ~ 42 calendar days
  const from = new Date(today.getTime() - 42 * 24 * 60 * 60 * 1000)
  return { from: toDateString(from), to: toDateString(today) }
}

export interface UseEodTimelineResult {
  entries: EodTimelineEntryDto[]
  loading: boolean
  error: string | null
  from: string
  to: string
  setFrom: (date: string) => void
  setTo: (date: string) => void
  refresh: () => void
}

export function useEodTimeline(portfolioId: string | null): UseEodTimelineResult {
  const defaults = defaultDateRange()
  const [entries, setEntries] = useState<EodTimelineEntryDto[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [from, setFrom] = useState(defaults.from)
  const [to, setTo] = useState(defaults.to)
  const [refreshSignal, setRefreshSignal] = useState(0)

  const refresh = useCallback(() => {
    setRefreshSignal((prev) => prev + 1)
  }, [])

  const abortRef = useRef<AbortController | null>(null)

  useEffect(() => {
    if (!portfolioId) {
      setEntries([])
      setLoading(false)
      setError(null)
      return
    }

    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller

    setLoading(true)
    setError(null)

    fetchEodTimeline(portfolioId, from, to)
      .then((data) => {
        if (controller.signal.aborted) return
        setEntries(data.entries)
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setError(err instanceof Error ? err.message : String(err))
        setEntries([])
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })

    return () => {
      controller.abort()
    }
  }, [portfolioId, from, to, refreshSignal])

  return { entries, loading, error, from, to, setFrom, setTo, refresh }
}
