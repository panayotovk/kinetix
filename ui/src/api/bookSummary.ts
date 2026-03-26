import type { BookAggregationDto } from '../types'
import { authFetch } from '../auth/authFetch'

export async function fetchBookSummary(
  bookId: string,
  baseCurrency: string,
): Promise<BookAggregationDto> {
  const response = await authFetch(
    `/api/v1/books/${encodeURIComponent(bookId)}/summary?baseCurrency=${encodeURIComponent(baseCurrency)}`,
  )
  if (!response.ok) {
    throw new Error(
      `Failed to fetch book summary: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}
