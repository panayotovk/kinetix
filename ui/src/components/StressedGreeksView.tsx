import { ArrowUp, ArrowDown } from 'lucide-react'
import type { StressedGreeksDto } from '../types'

function changeColorClass(value: number): string {
  if (value < 0) return 'text-green-600 dark:text-green-400'
  if (value > 0) return 'text-red-600 dark:text-red-400'
  return 'text-slate-500 dark:text-slate-400'
}

function ChangeIcon({ value }: { value: number }) {
  if (value < 0) return <ArrowDown className="inline h-3.5 w-3.5" />
  if (value > 0) return <ArrowUp className="inline h-3.5 w-3.5" />
  return null
}

function formatNum(v: string | number): string {
  const n = typeof v === 'string' ? Number(v) : v
  return n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function pctChange(base: number, stressed: number): string {
  if (base === 0) return 'N/A'
  const pct = ((stressed - base) / Math.abs(base)) * 100
  const sign = pct > 0 ? '+' : ''
  return `${sign}${pct.toFixed(1)}%`
}

function hasSignFlip(base: number, stressed: number): boolean {
  return (base > 0 && stressed < 0) || (base < 0 && stressed > 0)
}

interface GreekRow {
  label: string
  base: number
  stressed: number
}

interface StressedGreeksViewProps {
  greeks: StressedGreeksDto | undefined
}

export function StressedGreeksView({ greeks }: StressedGreeksViewProps) {
  if (!greeks) {
    return (
      <div className="text-sm text-slate-500 dark:text-slate-400 py-4">
        No stressed Greeks data available for this scenario.
      </div>
    )
  }

  const rows: GreekRow[] = [
    { label: 'Delta', base: Number(greeks.baseDelta), stressed: Number(greeks.stressedDelta) },
    { label: 'Gamma', base: Number(greeks.baseGamma), stressed: Number(greeks.stressedGamma) },
    { label: 'Vega', base: Number(greeks.baseVega), stressed: Number(greeks.stressedVega) },
    { label: 'Theta', base: Number(greeks.baseTheta), stressed: Number(greeks.stressedTheta) },
    { label: 'Rho', base: Number(greeks.baseRho), stressed: Number(greeks.stressedRho) },
  ]

  return (
    <div data-testid="greeks-view">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-slate-200 dark:border-slate-700 text-slate-500 dark:text-slate-400">
            <th className="py-2 text-left font-medium">Greek</th>
            <th className="py-2 text-right font-medium">Base</th>
            <th className="py-2 text-right font-medium">Stressed</th>
            <th className="py-2 text-right font-medium">Change</th>
            <th className="py-2 text-right font-medium">% Change</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const change = row.stressed - row.base
            const signFlip = hasSignFlip(row.base, row.stressed)

            return (
              <tr
                key={row.label}
                data-testid={`greek-row-${row.label}`}
                className={`border-b border-slate-100 dark:border-slate-800 ${
                  signFlip ? 'bg-amber-50 dark:bg-amber-900/20' : ''
                }`}
              >
                <td className="py-2 font-medium text-slate-900 dark:text-slate-100">
                  {row.label}
                  {signFlip && (
                    <span className="ml-2 text-xs px-1.5 py-0.5 rounded bg-amber-200 text-amber-800 dark:bg-amber-800 dark:text-amber-200 font-semibold">
                      SIGN FLIP
                    </span>
                  )}
                </td>
                <td className="py-2 text-right text-slate-700 dark:text-slate-300 tabular-nums">
                  {formatNum(row.base)}
                </td>
                <td className="py-2 text-right text-slate-700 dark:text-slate-300 tabular-nums">
                  {formatNum(row.stressed)}
                </td>
                <td className={`py-2 text-right font-medium tabular-nums ${changeColorClass(change)}`}>
                  <ChangeIcon value={change} /> {formatNum(change)}
                </td>
                <td className={`py-2 text-right font-medium tabular-nums ${changeColorClass(change)}`}>
                  {pctChange(row.base, row.stressed)}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
