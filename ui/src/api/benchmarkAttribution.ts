export interface BrinsonSectorDto {
  sectorLabel: string
  portfolioWeight: number
  benchmarkWeight: number
  portfolioReturn: number
  benchmarkReturn: number
  allocationEffect: number
  selectionEffect: number
  interactionEffect: number
  totalActiveContribution: number
}

export interface BrinsonAttributionDto {
  bookId: string
  benchmarkId: string
  asOfDate: string
  sectors: BrinsonSectorDto[]
  totalActiveReturn: number
  totalAllocationEffect: number
  totalSelectionEffect: number
  totalInteractionEffect: number
}

export async function fetchBrinsonAttribution(
  bookId: string,
  benchmarkId: string,
  asOfDate?: string,
): Promise<BrinsonAttributionDto> {
  const params = new URLSearchParams({ benchmarkId })
  if (asOfDate) params.set('asOfDate', asOfDate)

  const response = await fetch(`/api/v1/books/${bookId}/attribution?${params}`)
  if (!response.ok) {
    const text = await response.text().catch(() => response.statusText)
    throw new Error(`Failed to fetch attribution: ${response.status} ${text}`)
  }
  return response.json()
}
