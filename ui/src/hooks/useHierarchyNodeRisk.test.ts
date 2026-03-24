import { renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach, vi } from 'vitest'
import { useHierarchyNodeRisk } from './useHierarchyNodeRisk'

vi.mock('../api/hierarchyRisk', () => ({
  fetchHierarchyNodeRisk: vi.fn(),
}))

import { fetchHierarchyNodeRisk } from '../api/hierarchyRisk'

const mockFetch = vi.mocked(fetchHierarchyNodeRisk)

const FIRM_NODE = {
  level: 'FIRM',
  entityId: 'FIRM',
  entityName: 'FIRM',
  parentId: null,
  varValue: '2500000.00',
  expectedShortfall: '3125000.00',
  pnlToday: null,
  limitUtilisation: null,
  marginalVar: null,
  incrementalVar: null,
  topContributors: [
    { entityId: 'div-equities', entityName: 'Equities', varContribution: '1500000.00', pctOfTotal: '60.00' },
    { entityId: 'div-rates', entityName: 'Rates', varContribution: '1000000.00', pctOfTotal: '40.00' },
  ],
  childCount: 2,
  isPartial: false,
  missingBooks: [],
}

describe('useHierarchyNodeRisk', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockFetch.mockResolvedValue(FIRM_NODE)
  })

  it('returns null node when level is null', async () => {
    const { result } = renderHook(() => useHierarchyNodeRisk(null, 'FIRM'))

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(result.current.node).toBeNull()
    expect(mockFetch).not.toHaveBeenCalled()
  })

  it('fetches hierarchy node when level and entityId are provided', async () => {
    const { result } = renderHook(() => useHierarchyNodeRisk('FIRM', 'FIRM'))

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(mockFetch).toHaveBeenCalledWith('FIRM', 'FIRM')
    expect(result.current.node).toEqual(FIRM_NODE)
  })

  it('fetches desk-level node correctly', async () => {
    const deskNode = { ...FIRM_NODE, level: 'DESK', entityId: 'desk-rates', parentId: 'div-rates' }
    mockFetch.mockResolvedValue(deskNode)

    const { result } = renderHook(() => useHierarchyNodeRisk('DESK', 'desk-rates'))

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(mockFetch).toHaveBeenCalledWith('DESK', 'desk-rates')
    expect(result.current.node).toEqual(deskNode)
  })

  it('sets node to null when API returns 404', async () => {
    mockFetch.mockResolvedValue(null)

    const { result } = renderHook(() => useHierarchyNodeRisk('FIRM', 'FIRM'))

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(result.current.node).toBeNull()
    expect(result.current.error).toBeNull()
  })

  it('sets error when fetch fails', async () => {
    mockFetch.mockRejectedValue(new Error('Network failure'))

    const { result } = renderHook(() => useHierarchyNodeRisk('FIRM', 'FIRM'))

    await waitFor(() => expect(result.current.loading).toBe(false))

    expect(result.current.error).toBe('Network failure')
    expect(result.current.node).toBeNull()
  })

  it('re-fetches when level changes', async () => {
    const { result, rerender } = renderHook(
      ({ level, entityId }: { level: string | null; entityId: string }) =>
        useHierarchyNodeRisk(level, entityId),
      { initialProps: { level: 'FIRM', entityId: 'FIRM' } },
    )

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(mockFetch).toHaveBeenCalledTimes(1)

    rerender({ level: 'DIVISION', entityId: 'div-equities' })

    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(mockFetch).toHaveBeenCalledTimes(2)
    expect(mockFetch).toHaveBeenLastCalledWith('DIVISION', 'div-equities')
  })

  it('enters loading state while fetch is in flight', async () => {
    let resolvePromise!: (value: null) => void
    mockFetch.mockImplementation(() => new Promise((resolve) => { resolvePromise = resolve }))

    const { result } = renderHook(() => useHierarchyNodeRisk('FIRM', 'FIRM'))

    await waitFor(() => expect(result.current.loading).toBe(true))
    expect(result.current.node).toBeNull()

    resolvePromise(null)
    await waitFor(() => expect(result.current.loading).toBe(false))
  })
})
