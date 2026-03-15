import { useState } from 'react'
import { Button } from './ui'

interface EodDateRangePickerProps {
  from: string
  to: string
  onRangeChange: (from: string, to: string) => void
}

type PresetKey = '1W' | '1M' | '3M' | 'YTD' | 'Custom'

function toDateString(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function resolvePreset(key: PresetKey): { from: string; to: string } {
  const today = new Date()
  const to = toDateString(today)
  switch (key) {
    case '1W': {
      const from = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000)
      return { from: toDateString(from), to }
    }
    case '1M': {
      const from = new Date(today.getTime() - 42 * 24 * 60 * 60 * 1000)
      return { from: toDateString(from), to }
    }
    case '3M': {
      const from = new Date(today.getTime() - 126 * 24 * 60 * 60 * 1000)
      return { from: toDateString(from), to }
    }
    case 'YTD': {
      const from = new Date(today.getFullYear(), 0, 1)
      return { from: toDateString(from), to }
    }
    default:
      return { from: toDateString(today), to }
  }
}

const PRESETS: { key: Exclude<PresetKey, 'Custom'>; label: string }[] = [
  { key: '1W', label: '1W' },
  { key: '1M', label: '1M' },
  { key: '3M', label: '3M' },
  { key: 'YTD', label: 'YTD' },
]

export function EodDateRangePicker({ from, to, onRangeChange }: EodDateRangePickerProps) {
  const [activePreset, setActivePreset] = useState<PresetKey>('1M')
  const [customFrom, setCustomFrom] = useState(from)
  const [customTo, setCustomTo] = useState(to)

  const handlePreset = (key: Exclude<PresetKey, 'Custom'>) => {
    setActivePreset(key)
    const range = resolvePreset(key)
    onRangeChange(range.from, range.to)
  }

  const handleCustomToggle = () => {
    if (activePreset === 'Custom') return
    setActivePreset('Custom')
    setCustomFrom(from)
    setCustomTo(to)
  }

  const handleCustomApply = () => {
    if (!customFrom || !customTo) return
    onRangeChange(customFrom, customTo)
  }

  return (
    <div data-testid="eod-date-range-picker" className="flex flex-col gap-2">
      <div className="flex items-center gap-2 flex-wrap">
        {PRESETS.map((preset) => (
          <Button
            key={preset.key}
            data-testid={`eod-preset-${preset.key}`}
            variant={activePreset === preset.key ? 'primary' : 'secondary'}
            size="sm"
            onClick={() => handlePreset(preset.key)}
          >
            {preset.label}
          </Button>
        ))}
        <Button
          data-testid="eod-preset-Custom"
          variant={activePreset === 'Custom' ? 'primary' : 'secondary'}
          size="sm"
          onClick={handleCustomToggle}
        >
          Custom
        </Button>
        {activePreset !== 'Custom' && (
          <span className="text-xs text-slate-500 dark:text-slate-400 ml-1" data-testid="eod-range-label">
            {from} — {to}
          </span>
        )}
      </div>

      {activePreset === 'Custom' && (
        <div data-testid="eod-custom-range-inputs" className="flex items-center gap-2 flex-wrap">
          <label className="text-xs text-slate-500 dark:text-slate-400">From</label>
          <input
            data-testid="eod-custom-from"
            type="date"
            value={customFrom}
            onChange={(e) => setCustomFrom(e.target.value)}
            className="text-xs border border-slate-200 dark:border-surface-600 bg-white dark:bg-surface-800 text-slate-800 dark:text-slate-200 rounded px-2 py-1"
          />
          <label className="text-xs text-slate-500 dark:text-slate-400">To</label>
          <input
            data-testid="eod-custom-to"
            type="date"
            value={customTo}
            onChange={(e) => setCustomTo(e.target.value)}
            className="text-xs border border-slate-200 dark:border-surface-600 bg-white dark:bg-surface-800 text-slate-800 dark:text-slate-200 rounded px-2 py-1"
          />
          <Button
            data-testid="eod-custom-apply"
            variant="primary"
            size="sm"
            onClick={handleCustomApply}
          >
            Apply
          </Button>
        </div>
      )}
    </div>
  )
}
