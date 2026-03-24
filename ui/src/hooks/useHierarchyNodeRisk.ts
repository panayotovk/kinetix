import { useEffect, useState } from 'react'
import { fetchHierarchyNodeRisk } from '../api/hierarchyRisk'
import type { HierarchyNodeRiskDto } from '../types'

export interface UseHierarchyNodeRiskResult {
  node: HierarchyNodeRiskDto | null
  loading: boolean
  error: string | null
}

export function useHierarchyNodeRisk(
  level: string | null,
  entityId: string,
): UseHierarchyNodeRiskResult {
  const [node, setNode] = useState<HierarchyNodeRiskDto | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!level) return

    let cancelled = false

    async function fetchNode() {
      setLoading(true)
      setError(null)
      try {
        const data = await fetchHierarchyNodeRisk(level!, entityId)
        if (!cancelled) setNode(data)
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    fetchNode()

    return () => {
      cancelled = true
    }
  }, [level, entityId])

  return { node, loading, error }
}
