import type { HierarchyNodeRiskDto } from '../types'
import { authFetch } from '../auth/authFetch'

export async function fetchHierarchyNodeRisk(
  level: string,
  entityId: string,
): Promise<HierarchyNodeRiskDto | null> {
  const response = await authFetch(
    `/api/v1/risk/hierarchy/${encodeURIComponent(level)}/${encodeURIComponent(entityId)}`,
  )
  if (response.status === 404) return null
  if (!response.ok) {
    throw new Error(
      `Failed to fetch hierarchy risk for ${level}/${entityId}: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}
