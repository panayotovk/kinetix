import type {
  HistoricalReplayRequestDto,
  HistoricalReplayResultDto,
  ReverseStressRequestDto,
  ReverseStressResultDto,
} from '../types'

export async function runHistoricalReplay(
  bookId: string,
  request: HistoricalReplayRequestDto,
): Promise<HistoricalReplayResultDto> {
  const response = await fetch(
    `/api/v1/risk/stress/${encodeURIComponent(bookId)}/historical-replay`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    },
  )
  if (!response.ok) {
    throw new Error(`Historical replay failed: ${response.status} ${response.statusText}`)
  }
  return response.json()
}

export async function runReverseStress(
  bookId: string,
  request: ReverseStressRequestDto,
): Promise<ReverseStressResultDto> {
  const response = await fetch(
    `/api/v1/risk/stress/${encodeURIComponent(bookId)}/reverse`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    },
  )
  if (!response.ok) {
    throw new Error(`Reverse stress failed: ${response.status} ${response.statusText}`)
  }
  return response.json()
}
