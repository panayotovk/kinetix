import type { PositionDto, PositionRiskDto, StrategyGroupDto } from '../types'

/**
 * Groups positions by strategyId and computes aggregated risk and P&L for each group.
 *
 * Returns:
 *   - groups: one StrategyGroupDto per unique strategyId present in the positions
 *   - ungrouped: positions that have no strategyId
 */
export function buildStrategyGroups(
  positions: PositionDto[],
  positionRisk: PositionRiskDto[] | undefined,
): { groups: StrategyGroupDto[]; ungrouped: PositionDto[] } {
  const riskByInstrument = new Map<string, PositionRiskDto>(
    (positionRisk ?? []).map((r) => [r.instrumentId, r]),
  )

  const strategyMap = new Map<string, PositionDto[]>()
  const ungrouped: PositionDto[] = []

  for (const pos of positions) {
    if (pos.strategyId) {
      const existing = strategyMap.get(pos.strategyId) ?? []
      existing.push(pos)
      strategyMap.set(pos.strategyId, existing)
    } else {
      ungrouped.push(pos)
    }
  }

  const groups: StrategyGroupDto[] = []
  for (const [strategyId, legs] of strategyMap.entries()) {
    const firstLeg = legs[0]

    // Net P&L: sum unrealized P&L across all legs
    const netPnl = legs
      .reduce((sum, leg) => sum + Number(leg.unrealizedPnl.amount), 0)
      .toFixed(2)

    // Net Greeks: sum across leg risk data (may be absent)
    let netDelta: number | null = null
    let netGamma: number | null = null
    let netVega: number | null = null

    for (const leg of legs) {
      const risk = riskByInstrument.get(leg.instrumentId)
      if (risk?.delta != null) {
        netDelta = (netDelta ?? 0) + Number(risk.delta)
      }
      if (risk?.gamma != null) {
        netGamma = (netGamma ?? 0) + Number(risk.gamma)
      }
      if (risk?.vega != null) {
        netVega = (netVega ?? 0) + Number(risk.vega)
      }
    }

    groups.push({
      strategyId,
      strategyType: firstLeg.strategyType ?? 'CUSTOM',
      strategyName: firstLeg.strategyName,
      legs,
      netDelta: netDelta != null ? netDelta.toFixed(4) : null,
      netGamma: netGamma != null ? netGamma.toFixed(4) : null,
      netVega: netVega != null ? netVega.toFixed(4) : null,
      netPnl,
    })
  }

  return { groups, ungrouped }
}
