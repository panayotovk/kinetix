import { ChevronRight, ChevronDown } from 'lucide-react'
import type { StressTestResultDto } from '../types'
import { formatCurrency } from '../utils/format'

interface ScenarioComparisonTableProps {
  results: StressTestResultDto[]
  selectedScenario: string | null
  onSelectScenario: (scenario: string | null) => void
}

function formatMultiplier(baseVar: string, stressedVar: string): string {
  const base = Number(baseVar)
  const stressed = Number(stressedVar)
  if (base === 0) return '-'
  return `${(stressed / base).toFixed(1)}x`
}

function breachSummary(result: StressTestResultDto): { count: number; worst: string } {
  const breaches = result.limitBreaches ?? []
  if (breaches.length === 0) return { count: 0, worst: 'OK' }
  const hasBreached = breaches.some((b) => b.breachSeverity === 'BREACHED')
  const hasWarning = breaches.some((b) => b.breachSeverity === 'WARNING')
  const worst = hasBreached ? 'BREACHED' : hasWarning ? 'WARNING' : 'OK'
  const count = breaches.filter((b) => b.breachSeverity !== 'OK').length
  return { count, worst }
}

const BREACH_BADGE_STYLES: Record<string, string> = {
  OK: 'bg-green-100 text-green-800',
  WARNING: 'bg-yellow-100 text-yellow-800',
  BREACHED: 'bg-red-100 text-red-800',
}

const BREACH_BORDER: Record<string, string> = {
  BREACHED: 'border-l-4 border-l-red-500',
  WARNING: 'border-l-4 border-l-amber-400',
}

export function ScenarioComparisonTable({
  results,
  selectedScenario,
  onSelectScenario,
}: ScenarioComparisonTableProps) {
  if (results.length === 0) {
    return (
      <p data-testid="no-results" className="text-sm text-slate-500">
        No stress test results yet. Click &quot;Run All Scenarios&quot; to see the comparison.
      </p>
    )
  }

  const hasAnyBreaches = results.some((r) => (r.limitBreaches ?? []).length > 0)

  return (
    <div data-testid="scenario-comparison-table">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b text-left text-slate-600 dark:text-slate-400">
            <th className="py-2 w-8"></th>
            <th className="py-2">Scenario</th>
            <th className="py-2 text-right">Base VaR</th>
            <th className="py-2 text-right">Stressed VaR</th>
            <th className="py-2 text-right">VaR Multiplier</th>
            <th className="py-2 text-right">P&amp;L Impact</th>
            {hasAnyBreaches && <th className="py-2 text-center">Limits</th>}
          </tr>
        </thead>
        <tbody>
          {results.map((r) => {
            const isSelected = selectedScenario === r.scenarioName
            const pnlValue = Number(r.pnlImpact)
            const isLoss = pnlValue < 0
            const { count, worst } = breachSummary(r)
            const breachBorder = BREACH_BORDER[worst] || ''
            return (
              <tr
                key={r.scenarioName}
                data-testid="scenario-row"
                className={`border-b cursor-pointer transition-colors ${
                  isSelected
                    ? 'bg-indigo-50 dark:bg-indigo-900/20 border-l-2 border-l-indigo-500'
                    : breachBorder || 'hover:bg-slate-50 dark:hover:bg-slate-800'
                }`}
                onClick={() => onSelectScenario(isSelected ? null : r.scenarioName)}
              >
                <td className="py-1.5 text-slate-400">
                  {isSelected ? (
                    <ChevronDown className="h-4 w-4" />
                  ) : (
                    <ChevronRight className="h-4 w-4" />
                  )}
                </td>
                <td className="py-1.5 font-medium">{r.scenarioName.replace(/_/g, ' ')}</td>
                <td className="py-1.5 text-right">{formatCurrency(r.baseVar)}</td>
                <td className="py-1.5 text-right font-medium text-red-600 dark:text-red-400">
                  {formatCurrency(r.stressedVar)}
                </td>
                <td data-testid="var-multiplier" className="py-1.5 text-right font-medium">
                  {formatMultiplier(r.baseVar, r.stressedVar)}
                </td>
                <td
                  data-testid="pnl-impact"
                  className={`py-1.5 text-right font-medium ${isLoss ? 'text-red-600 dark:text-red-400' : ''}`}
                >
                  {formatCurrency(r.pnlImpact)}
                </td>
                {hasAnyBreaches && (
                  <td className="py-1.5 text-center" data-testid="breach-badge">
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${BREACH_BADGE_STYLES[worst]}`}
                    >
                      {count > 0 ? `${count} ${worst === 'BREACHED' ? 'breach' : 'warn'}` : 'OK'}
                    </span>
                  </td>
                )}
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
