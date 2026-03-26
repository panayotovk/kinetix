import type { RunManifestDto, ReplayResponseDto } from '../types'
import { authFetch } from '../auth/authFetch'

export async function fetchRunManifest(jobId: string): Promise<RunManifestDto | null> {
  const response = await authFetch(`/api/v1/risk/runs/${encodeURIComponent(jobId)}/manifest`)
  if (response.status === 404) return null
  if (!response.ok) {
    throw new Error(`Failed to fetch manifest: ${response.status} ${response.statusText}`)
  }
  return response.json()
}

export async function triggerReplay(jobId: string): Promise<ReplayResponseDto> {
  const response = await authFetch(`/api/v1/risk/runs/${encodeURIComponent(jobId)}/replay`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    const message = body.message || body.error || `Replay failed: ${response.status}`
    const err = new Error(message) as Error & { status: number; errorCode: string }
    err.status = response.status
    err.errorCode = body.error ?? ''
    throw err
  }
  return response.json()
}
