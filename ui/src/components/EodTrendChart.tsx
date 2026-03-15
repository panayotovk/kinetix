import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import type { EodTimelineEntryDto } from '../types'
import { formatCurrency } from '../utils/format'
import { formatCompactCurrency } from '../utils/formatCompactCurrency'
import { clampTooltipLeft } from '../utils/clampTooltipLeft'

interface EodTrendChartProps {
  entries: EodTimelineEntryDto[]
  selectedDate: string | null
  onSelectDate: (date: string) => void
  isLoading?: boolean
}

const PADDING = { top: 32, right: 16, bottom: 32, left: 56 }
const CHART_HEIGHT = 220
const DEFAULT_WIDTH = 600

function computeNiceGridLines(min: number, max: number, count: number): number[] {
  const range = max - min
  if (range === 0) return [min]

  const rough = range / count
  const magnitude = Math.pow(10, Math.floor(Math.log10(rough)))
  const candidates = [1, 2, 2.5, 5, 10]
  let step = candidates[candidates.length - 1] * magnitude
  for (const c of candidates) {
    if (c * magnitude >= rough) {
      step = c * magnitude
      break
    }
  }

  const lines: number[] = []
  const start = Math.ceil(min / step) * step
  for (let v = start; v <= max; v += step) {
    lines.push(v)
  }

  if (lines.length < 2 && range > 0) {
    lines.length = 0
    const simpleStep = range / count
    for (let i = 1; i <= count; i++) {
      lines.push(min + simpleStep * i)
    }
  }

  return lines
}

// Build path segments broken at null values — so missing dates create gaps
function buildSegmentedPath(
  points: Array<{ x: number; y: number } | null>,
): string[] {
  const paths: string[] = []
  let current = ''
  for (const pt of points) {
    if (pt === null) {
      if (current) {
        paths.push(current)
        current = ''
      }
    } else {
      if (!current) {
        current = `M ${pt.x} ${pt.y}`
      } else {
        current += ` L ${pt.x} ${pt.y}`
      }
    }
  }
  if (current) paths.push(current)
  return paths
}

export function EodTrendChart({ entries, selectedDate, onSelectDate, isLoading }: EodTrendChartProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const tooltipRef = useRef<HTMLDivElement>(null)
  const [containerWidth, setContainerWidth] = useState(DEFAULT_WIDTH)
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null)
  const [tooltipLeft, setTooltipLeft] = useState(0)

  const validEntries = useMemo(() => entries.filter((e) => e.varValue !== null), [entries])
  const hasChart = validEntries.length >= 2

  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    const observer = new ResizeObserver((entries) => {
      for (const entry of entries) {
        setContainerWidth(entry.contentRect.width)
      }
    })
    observer.observe(el)
    setContainerWidth(el.clientWidth)
    return () => observer.disconnect()
  }, [hasChart])

  const plotWidth = containerWidth - PADDING.left - PADDING.right
  const plotHeight = CHART_HEIGHT - PADDING.top - PADDING.bottom

  // Compute y-axis extent across both VaR and ES (valid entries only)
  const { min, max } = useMemo(() => {
    const values: number[] = []
    for (const e of entries) {
      if (e.varValue !== null) values.push(e.varValue)
      if (e.expectedShortfall !== null) values.push(e.expectedShortfall)
    }
    if (values.length === 0) return { min: 0, max: 1 }
    const minVal = Math.min(...values)
    const maxVal = Math.max(...values)
    const range = maxVal - minVal
    const pad = range * 0.1 || maxVal * 0.1 || 1
    return { min: minVal - pad, max: maxVal + pad }
  }, [entries])

  const gridLines = useMemo(() => computeNiceGridLines(min, max, 4), [min, max])

  // X-axis: map by date index across ALL entries (including missing)
  const toX = useCallback(
    (index: number): number => {
      if (entries.length <= 1) return PADDING.left + plotWidth / 2
      return PADDING.left + (index / (entries.length - 1)) * plotWidth
    },
    [entries.length, plotWidth],
  )

  const toY = useCallback(
    (value: number): number => {
      const range = max - min || 1
      return PADDING.top + (1 - (value - min) / range) * plotHeight
    },
    [min, max, plotHeight],
  )

  // Build VaR and ES point arrays (null for missing entries)
  const varPoints = useMemo(
    () =>
      entries.map((e, i) =>
        e.varValue !== null ? { x: toX(i), y: toY(e.varValue) } : null,
      ),
    [entries, toX, toY],
  )

  const esPoints = useMemo(
    () =>
      entries.map((e, i) =>
        e.expectedShortfall !== null ? { x: toX(i), y: toY(e.expectedShortfall) } : null,
      ),
    [entries, toX, toY],
  )

  const varPaths = useMemo(() => buildSegmentedPath(varPoints), [varPoints])
  const esPaths = useMemo(() => buildSegmentedPath(esPoints), [esPoints])

  // X-axis labels: up to 6 evenly distributed dates
  const xLabels = useMemo(() => {
    if (entries.length < 2) return []
    const count = Math.min(6, entries.length)
    const labels: { x: number; text: string }[] = []
    for (let i = 0; i < count; i++) {
      const index = Math.round((i / (count - 1)) * (entries.length - 1))
      labels.push({
        x: toX(index),
        text: entries[index].valuationDate.slice(5), // MM-DD
      })
    }
    return labels
  }, [entries, toX])

  const handleMouseMove = useCallback(
    (e: React.MouseEvent<SVGSVGElement>) => {
      if (entries.length === 0) return
      const el = containerRef.current
      if (!el) return
      const rect = el.getBoundingClientRect()
      const mouseX = e.clientX - rect.left

      // Find nearest valid (non-null) entry by x distance
      let closest: number | null = null
      let closestDist = Infinity
      for (let i = 0; i < entries.length; i++) {
        if (entries[i].varValue === null) continue
        const px = toX(i)
        const dist = Math.abs(px - mouseX)
        if (dist < closestDist) {
          closestDist = dist
          closest = i
        }
      }
      setHoveredIndex(closest)
    },
    [entries, toX],
  )

  const handleMouseLeave = useCallback(() => {
    setHoveredIndex(null)
  }, [])

  const handleClick = useCallback(
    (e: React.MouseEvent<SVGSVGElement>) => {
      if (entries.length === 0) return
      const el = containerRef.current
      if (!el) return
      const rect = el.getBoundingClientRect()
      const mouseX = e.clientX - rect.left

      let closest: number | null = null
      let closestDist = Infinity
      for (let i = 0; i < entries.length; i++) {
        if (entries[i].varValue === null) continue
        const px = toX(i)
        const dist = Math.abs(px - mouseX)
        if (dist < closestDist) {
          closestDist = dist
          closest = i
        }
      }
      if (closest !== null) {
        onSelectDate(entries[closest].valuationDate)
      }
    },
    [entries, toX, onSelectDate],
  )

  useLayoutEffect(() => {
    if (hoveredIndex === null || !tooltipRef.current) return
    const tooltipWidth = tooltipRef.current.offsetWidth
    const pt = varPoints[hoveredIndex]
    const pointX = pt?.x ?? toX(hoveredIndex)
    setTooltipLeft(clampTooltipLeft(pointX, tooltipWidth, containerWidth))
  }, [hoveredIndex, varPoints, containerWidth, toX])

  if (isLoading && entries.length === 0) {
    return (
      <div data-testid="eod-trend-chart" className="rounded bg-white dark:bg-slate-800 border border-slate-200 dark:border-surface-700 p-4">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300">VaR / ES Trend</h3>
        </div>
        <div
          role="status"
          aria-label="Loading chart data"
          className="space-y-3 animate-pulse"
          style={{ height: CHART_HEIGHT }}
        >
          <div className="h-2 bg-slate-200 dark:bg-slate-700 rounded w-full" />
          <div className="h-2 bg-slate-200 dark:bg-slate-700 rounded w-full" />
          <div className="h-2 bg-slate-200 dark:bg-slate-700 rounded w-3/4" />
          <div className="h-2 bg-slate-200 dark:bg-slate-700 rounded w-full" />
        </div>
      </div>
    )
  }

  if (entries.length === 0) {
    return (
      <div data-testid="eod-trend-chart" className="rounded bg-white dark:bg-slate-800 border border-slate-200 dark:border-surface-700 p-4">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300">VaR / ES Trend</h3>
        </div>
        <div
          className="flex items-center justify-center text-sm text-slate-400 dark:text-slate-500"
          style={{ height: CHART_HEIGHT }}
          data-testid="eod-chart-empty"
        >
          No EOD history for this period.
        </div>
      </div>
    )
  }

  if (validEntries.length === 1) {
    const entry = validEntries[0]
    return (
      <div data-testid="eod-trend-chart" className="rounded bg-white dark:bg-slate-800 border border-slate-200 dark:border-surface-700 p-4">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300">VaR / ES Trend</h3>
          <span className="text-sm font-mono text-indigo-500 dark:text-indigo-400">
            {entry.varValue !== null ? formatCurrency(entry.varValue) : '—'}
          </span>
        </div>
        <div
          className="flex items-center justify-center text-sm text-slate-400 dark:text-slate-500"
          style={{ height: CHART_HEIGHT }}
          data-testid="eod-chart-single-point"
        >
          Only one data point — need at least two to draw a trend.
        </div>
      </div>
    )
  }

  const latestValid = validEntries[validEntries.length - 1]

  return (
    <div
      ref={containerRef}
      data-testid="eod-trend-chart"
      className="relative rounded bg-white dark:bg-slate-800 border border-slate-200 dark:border-surface-700 p-4 pb-14"
    >
      <div className="flex items-center justify-between mb-1">
        <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300">VaR / ES Trend</h3>
        <div className="flex items-center gap-3">
          <span className="text-sm font-mono text-indigo-500 dark:text-indigo-400">
            {latestValid.varValue !== null ? formatCurrency(latestValid.varValue) : '—'}
          </span>
          <span className="text-sm font-mono text-amber-500 dark:text-amber-400">
            {latestValid.expectedShortfall !== null ? formatCurrency(latestValid.expectedShortfall) : '—'}
          </span>
        </div>
      </div>

      <div className="flex items-center gap-3 mb-1 text-xs text-slate-500 dark:text-slate-400">
        <span className="flex items-center gap-1">
          <span className="inline-block w-3 h-0.5 bg-indigo-500 rounded" />
          VaR
        </span>
        <span className="flex items-center gap-1">
          <span className="inline-block w-3 h-0.5 bg-amber-500 rounded" />
          ES
        </span>
      </div>

      <svg
        width="100%"
        height={CHART_HEIGHT}
        className="select-none cursor-pointer"
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
        onClick={handleClick}
        aria-label="EOD VaR and ES trend chart"
        role="img"
      >
        {/* Y-axis grid lines */}
        {gridLines.map((v) => {
          const y = toY(v)
          return (
            <g key={v}>
              <line
                x1={PADDING.left}
                y1={y}
                x2={containerWidth - PADDING.right}
                y2={y}
                stroke="#334155"
                strokeDasharray="4 2"
              />
              <text x={PADDING.left - 6} y={y + 3} textAnchor="end" fill="#94a3b8" fontSize={10}>
                {formatCompactCurrency(v)}
              </text>
            </g>
          )
        })}

        {/* X-axis labels */}
        {xLabels.map((label, i) => (
          <text
            key={i}
            x={label.x}
            y={CHART_HEIGHT - 6}
            textAnchor="middle"
            fill="#94a3b8"
            fontSize={10}
          >
            {label.text}
          </text>
        ))}

        {/* ES segmented paths */}
        {esPaths.map((d, i) => (
          <path
            key={`es-${i}`}
            d={d}
            fill="none"
            stroke="#f59e0b"
            strokeWidth={1.5}
            strokeLinejoin="round"
          />
        ))}

        {/* VaR segmented paths */}
        {varPaths.map((d, i) => (
          <path
            key={`var-${i}`}
            d={d}
            fill="none"
            stroke="#6366f1"
            strokeWidth={2.5}
            strokeLinejoin="round"
          />
        ))}

        {/* Gap markers: red X at missing entries */}
        {entries.map((e, i) => {
          if (e.varValue !== null) return null
          const x = toX(i)
          const midY = PADDING.top + plotHeight / 2
          return (
            <g key={`gap-${i}`} aria-label={`Missing EOD for ${e.valuationDate}`}>
              <line x1={x - 4} y1={midY - 4} x2={x + 4} y2={midY + 4} stroke="#ef4444" strokeWidth={1.5} />
              <line x1={x + 4} y1={midY - 4} x2={x - 4} y2={midY + 4} stroke="#ef4444" strokeWidth={1.5} />
            </g>
          )
        })}

        {/* Data point dots */}
        {entries.map((e, i) => {
          if (e.varValue === null) return null
          const pt = varPoints[i]
          if (!pt) return null
          const isSelected = e.valuationDate === selectedDate
          const isHovered = hoveredIndex === i
          return (
            <circle
              key={`dot-var-${i}`}
              cx={pt.x}
              cy={pt.y}
              r={isSelected ? 5 : isHovered ? 4 : 3}
              fill="#6366f1"
              stroke={isSelected ? 'white' : isHovered ? 'white' : 'transparent'}
              strokeWidth={isSelected ? 2.5 : isHovered ? 2 : 0}
              className="cursor-pointer"
              aria-label={`VaR ${formatCurrency(e.varValue)} on ${e.valuationDate}`}
            />
          )
        })}

        {/* Hover crosshair */}
        {hoveredIndex !== null && varPoints[hoveredIndex] && (
          <line
            data-testid="eod-crosshair"
            x1={varPoints[hoveredIndex]!.x}
            y1={PADDING.top}
            x2={varPoints[hoveredIndex]!.x}
            y2={PADDING.top + plotHeight}
            stroke="#94a3b8"
            strokeDasharray="4 2"
            strokeWidth={1}
          />
        )}
      </svg>

      {/* Tooltip */}
      {hoveredIndex !== null && entries[hoveredIndex] && (
        <div className="relative">
          <div
            ref={tooltipRef}
            data-testid="eod-chart-tooltip"
            className="absolute top-0 bg-slate-800 text-white text-xs rounded shadow-lg px-3 py-2 pointer-events-none whitespace-nowrap border border-slate-600 z-10"
            style={{ left: `${tooltipLeft}px` }}
          >
            <div className="font-medium mb-1">{entries[hoveredIndex].valuationDate}</div>
            {entries[hoveredIndex].varValue !== null ? (
              <div className="flex gap-3">
                <span className="text-indigo-400">
                  VaR: {formatCurrency(entries[hoveredIndex].varValue!)}
                </span>
                {entries[hoveredIndex].expectedShortfall !== null && (
                  <span className="text-amber-400">
                    ES: {formatCurrency(entries[hoveredIndex].expectedShortfall!)}
                  </span>
                )}
              </div>
            ) : (
              <div className="text-red-400">MISSING</div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
