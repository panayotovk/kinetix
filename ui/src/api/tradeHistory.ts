import type { TradeHistoryDto } from '../types'
import { authFetch } from '../auth/authFetch'

export async function fetchTradeHistory(
  bookId: string,
): Promise<TradeHistoryDto[]> {
  const response = await authFetch(
    `/api/v1/books/${encodeURIComponent(bookId)}/trades`,
  )
  if (!response.ok) {
    throw new Error(
      `Failed to fetch trade history: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}
