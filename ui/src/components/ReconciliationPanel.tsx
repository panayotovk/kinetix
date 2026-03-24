import { useEffect, useState } from 'react'
import { Inbox } from 'lucide-react'
import { fetchReconciliations } from '../api/execution'
import type { ReconciliationDto } from '../types'
import { Card, EmptyState } from './ui'

interface ReconciliationPanelProps {
  bookId: string | null
}

export function ReconciliationPanel({ bookId }: ReconciliationPanelProps) {
  const [reconciliations, setReconciliations] = useState<ReconciliationDto[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!bookId) return

    let cancelled = false

    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await fetchReconciliations(bookId!)
        if (!cancelled) setReconciliations(data)
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()

    return () => {
      cancelled = true
    }
  }, [bookId])

  if (!bookId) {
    return (
      <Card>
        <EmptyState icon={<Inbox className="h-10 w-10" />} title="Select a book to view reconciliation data." />
      </Card>
    )
  }

  if (loading) {
    return <p className="text-slate-500">Loading reconciliation data...</p>
  }

  if (error) {
    return <p className="text-red-600">{error}</p>
  }

  if (reconciliations.length === 0) {
    return (
      <Card>
        <EmptyState
          icon={<Inbox className="h-10 w-10" />}
          title="No reconciliation data for this book."
        />
      </Card>
    )
  }

  return (
    <div className="space-y-4">
      {reconciliations.map((recon) => {
        const hasBreaks = recon.breakCount > 0
        return (
          <Card key={recon.reconciliationDate}>
            {/* Reconciliation summary row */}
            <div
              data-testid={`recon-row-${recon.reconciliationDate}`}
              className={`flex items-center justify-between p-3 rounded-md mb-3 ${hasBreaks ? 'bg-amber-50' : 'bg-green-50'}`}
            >
              <div className="flex items-center gap-4">
                <span className="text-sm font-semibold text-slate-700">{recon.reconciliationDate}</span>
                <span
                  className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                    recon.status === 'CLEAN'
                      ? 'bg-green-100 text-green-800'
                      : 'bg-amber-100 text-amber-800'
                  }`}
                >
                  {recon.status}
                </span>
              </div>
              <div className="flex items-center gap-6 text-sm text-slate-600">
                <span>
                  Matched: <strong>{recon.matchedCount} / {recon.totalPositions}</strong>
                </span>
                {hasBreaks && (
                  <span className="text-amber-700 font-medium">{recon.breakCount} break(s)</span>
                )}
                <span className="text-xs text-slate-400">
                  {new Date(recon.reconciledAt).toLocaleString()}
                </span>
              </div>
            </div>

            {/* Break details table */}
            {hasBreaks && (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-amber-200" data-testid={`breaks-table-${recon.reconciliationDate}`}>
                  <thead>
                    <tr className="bg-amber-50">
                      <th className="px-4 py-2 text-left text-xs font-semibold text-amber-800">Instrument</th>
                      <th className="px-4 py-2 text-right text-xs font-semibold text-amber-800">Internal Qty</th>
                      <th className="px-4 py-2 text-right text-xs font-semibold text-amber-800">PB Qty</th>
                      <th className="px-4 py-2 text-right text-xs font-semibold text-amber-800">Break Qty</th>
                      <th className="px-4 py-2 text-right text-xs font-semibold text-amber-800">Break Notional</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-amber-100">
                    {recon.breaks.map((brk) => (
                      <tr
                        key={brk.instrumentId}
                        data-testid={`break-row-${brk.instrumentId}`}
                        className="bg-amber-50"
                      >
                        <td className="px-4 py-2 text-sm font-medium text-amber-900">{brk.instrumentId}</td>
                        <td className="px-4 py-2 text-sm text-right text-amber-800">{brk.internalQty}</td>
                        <td className="px-4 py-2 text-sm text-right text-amber-800">{brk.primeBrokerQty}</td>
                        <td className="px-4 py-2 text-sm text-right font-semibold text-amber-900">{brk.breakQty}</td>
                        <td className="px-4 py-2 text-sm text-right text-amber-900">{brk.breakNotional}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        )
      })}
    </div>
  )
}
