import type { ExecutionCostDto, ReconciliationDto } from '../types'
import { authFetch } from '../auth/authFetch'

export async function fetchExecutionCosts(bookId: string): Promise<ExecutionCostDto[]> {
  const response = await authFetch(`/api/v1/execution/cost/${encodeURIComponent(bookId)}`)
  if (!response.ok) {
    throw new Error(`Failed to fetch execution costs: ${response.status} ${response.statusText}`)
  }
  return response.json()
}

export async function fetchReconciliations(bookId: string): Promise<ReconciliationDto[]> {
  const response = await authFetch(`/api/v1/execution/reconciliation/${encodeURIComponent(bookId)}`)
  if (!response.ok) {
    throw new Error(`Failed to fetch reconciliations: ${response.status} ${response.statusText}`)
  }
  return response.json()
}
