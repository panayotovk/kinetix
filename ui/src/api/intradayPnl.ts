import type { IntradayPnlSeriesDto } from '../types'

export async function fetchIntradayPnl(
  bookId: string,
  from: string,
  to: string,
): Promise<IntradayPnlSeriesDto> {
  const params = new URLSearchParams({ from, to })
  const response = await fetch(
    `/api/v1/risk/pnl/intraday/${encodeURIComponent(bookId)}?${params}`,
  )
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    const message = body.error || `Failed to fetch intraday P&L: ${response.status}`
    throw new Error(message)
  }
  return response.json()
}
