const FX_PREFIX = new Set(['FX'])

export function formatInstrumentTypeLabel(instrumentType: string): string {
  return instrumentType
    .split('_')
    .map((word) => {
      if (FX_PREFIX.has(word.toUpperCase())) return 'FX'
      return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
    })
    .join(' ')
}

export const INSTRUMENT_TYPE_COLORS: Record<string, string> = {
  CASH_EQUITY:         'bg-blue-100 text-blue-800',
  EQUITY_OPTION:       'bg-amber-100 text-amber-800',
  GOVERNMENT_BOND:     'bg-green-100 text-green-800',
  CORPORATE_BOND:      'bg-green-100 text-green-700',
  FX_SPOT:             'bg-purple-100 text-purple-800',
  FX_FORWARD:          'bg-purple-100 text-purple-700',
  INTEREST_RATE_SWAP:  'bg-teal-100 text-teal-800',
  FUTURES:             'bg-orange-100 text-orange-800',
  COMMODITY:           'bg-stone-100 text-stone-800',
  CREDIT_DEFAULT_SWAP: 'bg-rose-100 text-rose-800',
  CONVERTIBLE_BOND:    'bg-lime-100 text-lime-800',
}

// SVG hex colors for donut chart segments
export const INSTRUMENT_TYPE_SVG_COLORS: Record<string, string> = {
  CASH_EQUITY:         '#3b82f6',
  EQUITY_OPTION:       '#f59e0b',
  GOVERNMENT_BOND:     '#22c55e',
  CORPORATE_BOND:      '#16a34a',
  FX_SPOT:             '#a855f7',
  FX_FORWARD:          '#9333ea',
  INTEREST_RATE_SWAP:  '#14b8a6',
  FUTURES:             '#f97316',
  COMMODITY:           '#78716c',
  CREDIT_DEFAULT_SWAP: '#f43f5e',
  CONVERTIBLE_BOND:    '#84cc16',
}

// Map instrument type to the asset class expected by the backend API
export const INSTRUMENT_TYPE_TO_ASSET_CLASS: Record<string, string> = {
  CASH_EQUITY:         'EQUITY',
  EQUITY_OPTION:       'EQUITY',
  GOVERNMENT_BOND:     'FIXED_INCOME',
  CORPORATE_BOND:      'FIXED_INCOME',
  CONVERTIBLE_BOND:    'FIXED_INCOME',
  CREDIT_DEFAULT_SWAP: 'FIXED_INCOME',
  FX_SPOT:             'FX',
  FX_FORWARD:          'FX',
  INTEREST_RATE_SWAP:  'FIXED_INCOME',
  FUTURES:             'COMMODITY',
  COMMODITY:           'COMMODITY',
}
