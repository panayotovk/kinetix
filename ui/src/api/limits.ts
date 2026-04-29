import { authFetch } from '../auth/authFetch'

export type LimitLevel = 'FIRM' | 'DIVISION' | 'DESK' | 'BOOK' | 'TRADER' | 'COUNTERPARTY'

export type LimitTypeName =
  | 'POSITION'
  | 'NOTIONAL'
  | 'VAR'
  | 'CONCENTRATION'
  | 'ADV_CONCENTRATION'
  | 'VAR_BUDGET'

export interface LimitDefinitionDto {
  id: string
  level: LimitLevel
  entityId: string
  limitType: LimitTypeName
  limitValue: string
  intradayLimit: string | null
  overnightLimit: string | null
  active: boolean
}

export async function fetchLimits(): Promise<LimitDefinitionDto[]> {
  const response = await authFetch('/api/v1/limits')
  if (!response.ok) {
    throw new Error(`Failed to fetch limits: ${response.status} ${response.statusText}`)
  }
  return response.json()
}
