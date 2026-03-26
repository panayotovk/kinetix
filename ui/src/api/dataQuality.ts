import type { DataQualityStatus } from '../types'
import { authFetch } from '../auth/authFetch'

export async function fetchDataQualityStatus(): Promise<DataQualityStatus> {
  const response = await authFetch('/api/v1/data-quality/status')
  if (!response.ok) {
    throw new Error(
      `Failed to fetch data quality status: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}
