/**
 * Shared colour palette for P&L attribution factors.
 * Used by PnlAttributionTable, PnlWaterfallChart, and any future
 * component that visualises the same Greek/factor decomposition.
 */
export const PNL_FACTOR_COLORS: Record<string, string> = {
  delta: '#3b82f6',
  gamma: '#8b5cf6',
  vega: '#a855f7',
  theta: '#f59e0b',
  rho: '#22c55e',
  vanna: '#c084fc',
  volga: '#d946ef',
  charm: '#fb923c',
  crossGamma: '#a78bfa',
  unexplained: '#9ca3af',
  total: '#1e293b',
}
