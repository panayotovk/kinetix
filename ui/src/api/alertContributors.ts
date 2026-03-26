import { authFetch } from '../auth/authFetch'

export interface PositionContributor {
  instrumentId: string
  instrumentName?: string
  assetClass: string
  marketValue: string
  varContribution: string
  percentageOfTotal: string
  delta?: string
  gamma?: string
  vega?: string
  quantity?: string
}

export async function fetchAlertContributors(
  alertId: string,
): Promise<PositionContributor[]> {
  const response = await authFetch(
    `/api/v1/notifications/alerts/${encodeURIComponent(alertId)}/contributors`,
  )
  if (!response.ok) {
    throw new Error(
      `Failed to fetch contributors: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}
