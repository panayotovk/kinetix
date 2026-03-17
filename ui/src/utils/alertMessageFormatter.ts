import type { AlertEventDto } from '../types'

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(Math.round(value))
}

function severityPrefix(severity: string): string {
  if (severity === 'CRITICAL' || severity === 'WARNING') return `${severity}: `
  return ''
}

export function formatAlertMessage(alert: AlertEventDto): string {
  const prefix = severityPrefix(alert.severity)

  if (!Number.isFinite(alert.currentValue) || !Number.isFinite(alert.threshold)) {
    return `${prefix}${alert.message}`
  }

  const current = formatCurrency(alert.currentValue)
  const threshold = formatCurrency(alert.threshold)
  const book = alert.bookId

  switch (alert.type) {
    case 'PNL_THRESHOLD':
      return `${prefix}Daily P&L exceeded ${threshold} limit — current: ${current} (${book})`
    case 'VAR_BREACH':
      return `${prefix}VaR breached ${threshold} limit — current: ${current} (${book})`
    case 'CONCENTRATION': {
      const pct = alert.currentValue.toFixed(2)
      return `${prefix}Concentration exceeded ${alert.threshold}% limit — current: ${pct}% (${book})`
    }
    case 'DELTA_BREACH':
      return `${prefix}Delta breached ${threshold} limit — current: ${current} (${book})`
    case 'VEGA_BREACH':
      return `${prefix}Vega breached ${threshold} limit — current: ${current} (${book})`
    case 'MARGIN_BREACH':
      return `${prefix}Margin utilisation exceeded ${alert.threshold}% — current: ${alert.currentValue.toFixed(1)}% (${book})`
    case 'DATA_STALENESS':
      return `${prefix}Market data staleness detected — ${alert.currentValue.toFixed(0)}s since last update (${book})`
    case 'RISK_LIMIT':
      return `${prefix}Risk limit breached ${threshold} — current: ${current} (${book})`
    default:
      return `${prefix}${alert.message}`
  }
}
