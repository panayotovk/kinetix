import type { FactorRiskDto } from '../types'

export async function fetchLatestFactorRisk(
  bookId: string,
): Promise<FactorRiskDto | null> {
  const response = await fetch(
    `/api/v1/books/${encodeURIComponent(bookId)}/factor-risk/latest`,
  )
  if (response.status === 404) {
    return null
  }
  if (!response.ok) {
    throw new Error(
      `Failed to fetch factor risk: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}
