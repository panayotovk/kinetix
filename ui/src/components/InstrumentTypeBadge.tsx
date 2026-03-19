import { INSTRUMENT_TYPE_COLORS, formatInstrumentTypeLabel } from '../utils/instrumentTypes'

interface InstrumentTypeBadgeProps {
  instrumentType: string | undefined
  'data-testid'?: string
}

const FALLBACK_COLORS = 'bg-slate-100 text-slate-700'

export function InstrumentTypeBadge({ instrumentType, 'data-testid': testId }: InstrumentTypeBadgeProps) {
  if (!instrumentType) return null

  const colorClass = INSTRUMENT_TYPE_COLORS[instrumentType] ?? FALLBACK_COLORS
  const label = formatInstrumentTypeLabel(instrumentType)

  return (
    <span
      data-testid={testId}
      className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${colorClass}`}
    >
      {label}
    </span>
  )
}
