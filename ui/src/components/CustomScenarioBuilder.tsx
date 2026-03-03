import { useEffect } from 'react'
import { X, Zap, Save } from 'lucide-react'
import { useCustomScenario } from '../hooks/useCustomScenario'
import type { ScenarioSavePayload, ScenarioRunPayload } from '../hooks/useCustomScenario'
import { Button, Card, Input } from './ui'

const ASSET_CLASSES = ['EQUITY', 'FIXED_INCOME', 'COMMODITY', 'FX', 'DERIVATIVE'] as const

function formatAssetClassLabel(ac: string): string {
  return ac.replace(/_/g, ' ').toLowerCase()
}

interface CustomScenarioBuilderProps {
  open: boolean
  onClose: () => void
  onSave: (payload: ScenarioSavePayload) => void
  onRunAdHoc: (payload: ScenarioRunPayload) => void
  saving: boolean
  running: boolean
}

export function CustomScenarioBuilder({
  open,
  onClose,
  onSave,
  onRunAdHoc,
  saving,
  running,
}: CustomScenarioBuilderProps) {
  const scenario = useCustomScenario({ onSave, onRunAdHoc })

  useEffect(() => {
    if (!open) return

    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [open, onClose])

  if (!open) return null

  const hasName = scenario.name.trim().length > 0
  const hasErrors = Object.keys(scenario.errors).length > 0

  return (
    <>
      {/* Backdrop */}
      <div
        data-testid="scenario-builder-backdrop"
        className="fixed inset-0 z-40 bg-black/30"
        onClick={onClose}
      />
      <div
        data-testid="scenario-builder-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="scenario-builder-title"
        className="fixed top-0 right-0 h-full w-[420px] bg-white dark:bg-surface-800 border-l border-slate-200 dark:border-surface-700 shadow-xl z-50 flex flex-col"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-slate-200 dark:border-surface-700 bg-slate-50 dark:bg-surface-900">
          <h2
            id="scenario-builder-title"
            className="text-sm font-bold text-slate-800 dark:text-slate-200"
          >
            Custom Scenario
          </h2>
          <button
            data-testid="scenario-builder-close"
            aria-label="Close scenario builder"
            onClick={onClose}
            className="p-1 rounded hover:bg-slate-200 dark:hover:bg-surface-700 transition-colors"
          >
            <X className="h-4 w-4 text-slate-500 dark:text-slate-400" />
          </button>
        </div>

        {/* Scrollable content */}
        <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4">
          {/* Name & Description */}
          <Card>
            <div className="space-y-3">
              <div>
                <label
                  htmlFor="scenario-name"
                  className="block text-xs text-slate-500 dark:text-slate-400 mb-1"
                >
                  Scenario name
                </label>
                <Input
                  id="scenario-name"
                  aria-label="Scenario name"
                  data-testid="scenario-name"
                  value={scenario.name}
                  onChange={(e) => scenario.setName(e.target.value)}
                  placeholder="e.g. Rates Shock +200bp"
                  className="w-full"
                />
                {scenario.nameError && (
                  <p className="text-xs text-red-600 mt-0.5">{scenario.nameError}</p>
                )}
              </div>
              <div>
                <label
                  htmlFor="scenario-description"
                  className="block text-xs text-slate-500 dark:text-slate-400 mb-1"
                >
                  Description
                </label>
                <textarea
                  id="scenario-description"
                  aria-label="Scenario description"
                  data-testid="scenario-description"
                  value={scenario.description}
                  onChange={(e) => scenario.setDescription(e.target.value)}
                  placeholder="Describe the market conditions..."
                  rows={2}
                  className="w-full border border-slate-300 dark:border-surface-600 rounded-md px-3 py-1.5 text-sm bg-white dark:bg-surface-700 dark:text-slate-200 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 resize-none"
                />
              </div>
            </div>
          </Card>

          {/* Per-asset-class shocks */}
          {ASSET_CLASSES.map((ac) => {
            const label = formatAssetClassLabel(ac)
            return (
              <Card key={ac}>
                <div className="space-y-2">
                  <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                    {label}
                  </span>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label
                        htmlFor={`vol-shock-${ac}`}
                        className="block text-xs text-slate-500 dark:text-slate-400 mb-1"
                      >
                        Vol shock
                      </label>
                      <Input
                        id={`vol-shock-${ac}`}
                        aria-label={`Vol shock for ${label}`}
                        data-testid={`vol-shock-${ac}`}
                        type="number"
                        step="0.1"
                        min="0.5"
                        max="5.0"
                        value={scenario.volShocks[ac]}
                        onChange={(e) =>
                          scenario.setVolShock(ac, parseFloat(e.target.value) || 0)
                        }
                        className="w-full"
                      />
                    </div>
                    <div>
                      <label
                        htmlFor={`price-shock-${ac}`}
                        className="block text-xs text-slate-500 dark:text-slate-400 mb-1"
                      >
                        Price shock
                      </label>
                      <Input
                        id={`price-shock-${ac}`}
                        aria-label={`Price shock for ${label}`}
                        data-testid={`price-shock-${ac}`}
                        type="number"
                        step="0.05"
                        min="0.5"
                        max="1.5"
                        value={scenario.priceShocks[ac]}
                        onChange={(e) =>
                          scenario.setPriceShock(ac, parseFloat(e.target.value) || 0)
                        }
                        className="w-full"
                      />
                    </div>
                  </div>
                  {scenario.errors[ac] && (
                    <p className="text-xs text-red-600">{scenario.errors[ac]}</p>
                  )}
                </div>
              </Card>
            )
          })}
        </div>

        {/* Footer */}
        <div className="px-4 py-3 border-t border-slate-200 dark:border-surface-700 bg-slate-50 dark:bg-surface-900 flex gap-2">
          <Button
            data-testid="scenario-save-btn"
            variant="primary"
            onClick={scenario.save}
            loading={saving}
            disabled={!hasName || hasErrors || saving}
            className="flex-1"
            icon={<Save className="h-3.5 w-3.5" />}
          >
            Save &amp; Submit
          </Button>
          <Button
            data-testid="scenario-run-btn"
            variant="danger"
            onClick={scenario.runAdHoc}
            loading={running}
            disabled={hasErrors || running}
            icon={<Zap className="h-3.5 w-3.5" />}
          >
            Run Ad-Hoc
          </Button>
        </div>
      </div>
    </>
  )
}
