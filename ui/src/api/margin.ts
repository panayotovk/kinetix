import { authFetch } from '../auth/authFetch'

export interface MarginEstimateDto {
  initialMargin: string
  variationMargin: string
  totalMargin: string
  currency: string
}

export async function fetchMarginEstimate(
  bookId: string,
  previousMTM?: string,
): Promise<MarginEstimateDto | null> {
  const url = previousMTM != null
    ? `/api/v1/books/${encodeURIComponent(bookId)}/margin?previousMTM=${encodeURIComponent(previousMTM)}`
    : `/api/v1/books/${encodeURIComponent(bookId)}/margin`
  const response = await authFetch(url)
  if (response.status === 404) return null
  if (!response.ok) {
    throw new Error(`Failed to fetch margin estimate: ${response.status} ${response.statusText}`)
  }
  return response.json()
}
