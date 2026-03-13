import { useState, useCallback } from 'react'
import { fetchRunManifest, triggerReplay } from '../api/replay'
import type { RunManifestDto, ReplayResponseDto } from '../types'

type ReplayState = 'idle' | 'loading_manifest' | 'ready' | 'replaying' | 'completed' | 'error'

interface UseReplayResult {
  state: ReplayState
  manifest: RunManifestDto | null
  replayResult: ReplayResponseDto | null
  error: string | null
  errorCode: string | null
  loadManifest: (jobId: string) => Promise<void>
  replay: (jobId: string) => Promise<void>
  reset: () => void
}

export function useReplay(): UseReplayResult {
  const [state, setState] = useState<ReplayState>('idle')
  const [manifest, setManifest] = useState<RunManifestDto | null>(null)
  const [replayResult, setReplayResult] = useState<ReplayResponseDto | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [errorCode, setErrorCode] = useState<string | null>(null)

  const loadManifest = useCallback(async (jobId: string) => {
    setState('loading_manifest')
    setError(null)
    setErrorCode(null)
    try {
      const m = await fetchRunManifest(jobId)
      setManifest(m)
      setState(m ? 'ready' : 'idle')
    } catch (e: unknown) {
      const err = e as Error
      setState('error')
      setError(err.message)
    }
  }, [])

  const replay = useCallback(async (jobId: string) => {
    setState('replaying')
    setError(null)
    setErrorCode(null)
    try {
      const result = await triggerReplay(jobId)
      setReplayResult(result)
      setManifest(result.manifest)
      setState('completed')
    } catch (e: unknown) {
      const err = e as Error & { status?: number; errorCode?: string }
      setState('error')
      setError(err.message)
      setErrorCode(err.errorCode ?? null)
    }
  }, [])

  const reset = useCallback(() => {
    setState('idle')
    setManifest(null)
    setReplayResult(null)
    setError(null)
    setErrorCode(null)
  }, [])

  return { state, manifest, replayResult, error, errorCode, loadManifest, replay, reset }
}
