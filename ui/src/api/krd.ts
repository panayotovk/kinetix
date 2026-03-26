import type { KeyRateDurationResponseDto } from '../types'
import { authFetch } from '../auth/authFetch'

export async function fetchKrd(bookId: string): Promise<KeyRateDurationResponseDto> {
  const response = await authFetch(`/api/v1/risk/krd/${encodeURIComponent(bookId)}`)
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.error || `Failed to fetch KRD: ${response.status}`)
  }
  return response.json()
}
