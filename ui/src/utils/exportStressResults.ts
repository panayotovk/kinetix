import type { StressTestResultDto } from '../types'
import { exportToCsv } from './exportCsv'

function varMultiplier(base: string, stressed: string): string {
  const b = Number(base)
  const s = Number(stressed)
  if (b === 0) return 'N/A'
  return (s / b).toFixed(2) + 'x'
}

export function buildStressResultsCsv(results: StressTestResultDto[]): string {
  const header = 'Scenario,Base VaR,Stressed VaR,VaR Multiplier,P&L Impact,Calculated At'

  if (results.length === 0) return header

  const summaryRows = results.map(
    (r) =>
      `${r.scenarioName},${r.baseVar},${r.stressedVar},${varMultiplier(r.baseVar, r.stressedVar)},${r.pnlImpact},${r.calculatedAt}`,
  )

  const sections = [header, ...summaryRows]

  // Position details
  const hasPositions = results.some((r) => r.positionImpacts.length > 0)
  if (hasPositions) {
    sections.push('')
    sections.push('Position Details')
    sections.push(
      'Scenario,Instrument,Asset Class,Base Market Value,Stressed Market Value,P&L Impact,% of Total',
    )
    for (const r of results) {
      for (const p of r.positionImpacts) {
        sections.push(
          `${r.scenarioName},${p.instrumentId},${p.assetClass},${p.baseMarketValue},${p.stressedMarketValue},${p.pnlImpact},${p.percentageOfTotal}`,
        )
      }
    }
  }

  return sections.join('\n')
}

export function exportStressResultsToCsv(results: StressTestResultDto[]): void {
  const csv = buildStressResultsCsv(results)
  const headers = csv.split('\n')[0].split(',')
  const rows = csv
    .split('\n')
    .slice(1)
    .map((line) => line.split(','))

  exportToCsv('stress-test-results.csv', headers, rows)
}
