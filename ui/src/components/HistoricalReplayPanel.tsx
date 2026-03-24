import { History } from 'lucide-react'
import type { HistoricalReplayResultDto } from '../types'
import { formatCurrency } from '../utils/format'
import { Card, Button, Select, Spinner } from './ui'

interface HistoricalReplayPanelProps {
  scenarios: string[]
  result: HistoricalReplayResultDto | null
  loading: boolean
  error: string | null
  selectedScenario: string
  onScenarioChange: (scenario: string) => void
  onRun: () => void
  bookId: string | null
}

export function HistoricalReplayPanel({
  scenarios,
  result,
  loading,
  error,
  selectedScenario,
  onScenarioChange,
  onRun,
}: HistoricalReplayPanelProps) {
  return (
    <Card
      data-testid="historical-replay-panel"
      header={
        <span className="flex items-center gap-1.5">
          <History className="h-4 w-4" />
          Historical Scenario Replay
        </span>
      }
    >
      <div className="flex items-center gap-3 mb-4">
        <Select
          data-testid="replay-scenario-dropdown"
          value={selectedScenario}
          onChange={(e) => onScenarioChange(e.target.value)}
        >
          {scenarios.map((s) => (
            <option key={s} value={s}>
              {s.replace(/_/g, ' ')}
            </option>
          ))}
        </Select>
        <Button
          data-testid="replay-run-btn"
          variant="secondary"
          size="md"
          icon={<History className="h-3.5 w-3.5" />}
          onClick={onRun}
          loading={loading}
        >
          {loading ? 'Running...' : 'Run Replay'}
        </Button>
      </div>

      {loading && (
        <div data-testid="replay-loading" className="flex items-center gap-2 text-slate-500 text-sm">
          <Spinner size="sm" />
          Applying historical returns to current positions...
        </div>
      )}

      {error && (
        <div data-testid="replay-error" className="text-red-600 text-sm">
          {error}
        </div>
      )}

      {result && !loading && (
        <div data-testid="replay-results">
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div className="bg-slate-50 dark:bg-slate-800 rounded p-3">
              <p className="text-xs text-slate-500 mb-0.5">Scenario</p>
              <p className="font-semibold text-slate-900 dark:text-slate-100">
                {result.scenarioName.replace(/_/g, ' ')}
              </p>
            </div>
            <div className="bg-red-50 dark:bg-red-950 rounded p-3">
              <p className="text-xs text-slate-500 mb-0.5">Total P&L Impact</p>
              <p
                data-testid="replay-total-pnl"
                className="font-semibold text-red-600 dark:text-red-400 text-lg"
              >
                {formatCurrency(Number(result.totalPnlImpact))}
              </p>
            </div>
          </div>

          {(result.windowStart || result.windowEnd) && (
            <p className="text-xs text-slate-500 mb-3">
              Historical window:{' '}
              <span className="font-medium text-slate-700 dark:text-slate-300">
                {result.windowStart} → {result.windowEnd}
              </span>
            </p>
          )}

          <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300 mb-2">
            Position Impacts
          </h3>
          <div data-testid="replay-position-impacts" className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-slate-600 dark:text-slate-400">
                  <th className="py-2 pr-2">Instrument</th>
                  <th className="py-2 pr-2">Asset Class</th>
                  <th className="py-2 pr-2 text-right">Market Value</th>
                  <th className="py-2 text-right">P&L Impact</th>
                </tr>
              </thead>
              <tbody>
                {result.positionImpacts.map((impact) => (
                  <tr
                    key={impact.instrumentId}
                    className="border-b hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
                  >
                    <td className="py-1.5 pr-2 font-medium">
                      <span className="flex items-center gap-1.5">
                        {impact.instrumentId}
                        {impact.proxyUsed && (
                          <span
                            data-testid={`proxy-badge-${impact.instrumentId}`}
                            className="text-xs bg-amber-100 text-amber-700 px-1.5 py-0.5 rounded"
                            title="Asset class proxy returns used (instrument not in historical data)"
                          >
                            proxy
                          </span>
                        )}
                      </span>
                    </td>
                    <td className="py-1.5 pr-2 text-slate-600 dark:text-slate-400">
                      {impact.assetClass}
                    </td>
                    <td className="py-1.5 pr-2 text-right">
                      {formatCurrency(Number(impact.marketValue))}
                    </td>
                    <td className="py-1.5 text-right text-red-600 font-medium">
                      {formatCurrency(Number(impact.pnlImpact))}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </Card>
  )
}
