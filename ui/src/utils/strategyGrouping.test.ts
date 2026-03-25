import { describe, expect, it } from 'vitest'
import type { PositionDto, PositionRiskDto } from '../types'
import { buildStrategyGroups } from './strategyGrouping'

const makePosition = (overrides: Partial<PositionDto> = {}): PositionDto => ({
  bookId: 'book-1',
  instrumentId: 'AAPL',
  assetClass: 'EQUITY',
  quantity: '100',
  averageCost: { amount: '150.00', currency: 'USD' },
  marketPrice: { amount: '155.00', currency: 'USD' },
  marketValue: { amount: '15500.00', currency: 'USD' },
  unrealizedPnl: { amount: '500.00', currency: 'USD' },
  ...overrides,
})

const makeRisk = (overrides: Partial<PositionRiskDto> = {}): PositionRiskDto => ({
  instrumentId: 'AAPL',
  assetClass: 'EQUITY',
  marketValue: '15500.00',
  delta: '0.50',
  gamma: '0.02',
  vega: '10.00',
  theta: null,
  rho: null,
  varContribution: '800.00',
  esContribution: '1000.00',
  percentageOfTotal: '50.00',
  ...overrides,
})

describe('buildStrategyGroups', () => {
  it('places positions without strategyId into ungrouped', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL' }),
      makePosition({ instrumentId: 'GOOGL' }),
    ]

    const { groups, ungrouped } = buildStrategyGroups(positions, undefined)

    expect(groups).toHaveLength(0)
    expect(ungrouped).toHaveLength(2)
  })

  it('groups positions sharing a strategyId into a single group', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL-CALL', strategyId: 'strat-1', strategyType: 'STRADDLE' }),
      makePosition({ instrumentId: 'AAPL-PUT', strategyId: 'strat-1', strategyType: 'STRADDLE' }),
    ]

    const { groups, ungrouped } = buildStrategyGroups(positions, undefined)

    expect(groups).toHaveLength(1)
    expect(groups[0].strategyId).toBe('strat-1')
    expect(groups[0].legs).toHaveLength(2)
    expect(ungrouped).toHaveLength(0)
  })

  it('creates separate groups for different strategyIds', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL-CALL', strategyId: 'strat-1' }),
      makePosition({ instrumentId: 'AAPL-PUT', strategyId: 'strat-1' }),
      makePosition({ instrumentId: 'MSFT-CALL', strategyId: 'strat-2' }),
    ]

    const { groups } = buildStrategyGroups(positions, undefined)

    expect(groups).toHaveLength(2)
    const ids = groups.map((g) => g.strategyId)
    expect(ids).toContain('strat-1')
    expect(ids).toContain('strat-2')
  })

  it('computes netPnl as sum of unrealizedPnl across all legs', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL-CALL', strategyId: 'strat-1', unrealizedPnl: { amount: '300.00', currency: 'USD' } }),
      makePosition({ instrumentId: 'AAPL-PUT', strategyId: 'strat-1', unrealizedPnl: { amount: '200.00', currency: 'USD' } }),
    ]

    const { groups } = buildStrategyGroups(positions, undefined)

    expect(groups[0].netPnl).toBe('500.00')
  })

  it('computes netDelta as sum of delta values across all legs', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL-CALL', strategyId: 'strat-1' }),
      makePosition({ instrumentId: 'AAPL-PUT', strategyId: 'strat-1' }),
    ]
    const risk = [
      makeRisk({ instrumentId: 'AAPL-CALL', delta: '0.60' }),
      makeRisk({ instrumentId: 'AAPL-PUT', delta: '-0.40' }),
    ]

    const { groups } = buildStrategyGroups(positions, risk)

    expect(groups[0].netDelta).toBe('0.2000')
  })

  it('computes netGamma as sum of gamma values across all legs', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL-CALL', strategyId: 'strat-1' }),
      makePosition({ instrumentId: 'AAPL-PUT', strategyId: 'strat-1' }),
    ]
    const risk = [
      makeRisk({ instrumentId: 'AAPL-CALL', gamma: '0.03' }),
      makeRisk({ instrumentId: 'AAPL-PUT', gamma: '0.03' }),
    ]

    const { groups } = buildStrategyGroups(positions, risk)

    expect(groups[0].netGamma).toBe('0.0600')
  })

  it('computes netVega as sum of vega values across all legs', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL-CALL', strategyId: 'strat-1' }),
      makePosition({ instrumentId: 'AAPL-PUT', strategyId: 'strat-1' }),
    ]
    const risk = [
      makeRisk({ instrumentId: 'AAPL-CALL', vega: '15.00' }),
      makeRisk({ instrumentId: 'AAPL-PUT', vega: '12.00' }),
    ]

    const { groups } = buildStrategyGroups(positions, risk)

    expect(groups[0].netVega).toBe('27.0000')
  })

  it('returns null for netDelta when no risk data is provided', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL-CALL', strategyId: 'strat-1' }),
    ]

    const { groups } = buildStrategyGroups(positions, undefined)

    expect(groups[0].netDelta).toBeNull()
    expect(groups[0].netGamma).toBeNull()
    expect(groups[0].netVega).toBeNull()
  })

  it('returns null for a greek when all leg risk data is missing that field', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL-CALL', strategyId: 'strat-1' }),
    ]
    const risk = [
      makeRisk({ instrumentId: 'AAPL-CALL', delta: null, gamma: null, vega: null }),
    ]

    const { groups } = buildStrategyGroups(positions, risk)

    expect(groups[0].netDelta).toBeNull()
    expect(groups[0].netGamma).toBeNull()
    expect(groups[0].netVega).toBeNull()
  })

  it('derives strategyType from the first leg', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL-CALL', strategyId: 'strat-1', strategyType: 'STRADDLE' }),
      makePosition({ instrumentId: 'AAPL-PUT', strategyId: 'strat-1', strategyType: 'STRADDLE' }),
    ]

    const { groups } = buildStrategyGroups(positions, undefined)

    expect(groups[0].strategyType).toBe('STRADDLE')
  })

  it('falls back to CUSTOM when strategyType is absent from legs', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL-CALL', strategyId: 'strat-1', strategyType: undefined }),
    ]

    const { groups } = buildStrategyGroups(positions, undefined)

    expect(groups[0].strategyType).toBe('CUSTOM')
  })

  it('derives strategyName from the first leg', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL-CALL', strategyId: 'strat-1', strategyName: 'Sep Straddle' }),
      makePosition({ instrumentId: 'AAPL-PUT', strategyId: 'strat-1', strategyName: 'Sep Straddle' }),
    ]

    const { groups } = buildStrategyGroups(positions, undefined)

    expect(groups[0].strategyName).toBe('Sep Straddle')
  })

  it('handles a mix of grouped and ungrouped positions', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL-CALL', strategyId: 'strat-1' }),
      makePosition({ instrumentId: 'AAPL-PUT', strategyId: 'strat-1' }),
      makePosition({ instrumentId: 'MSFT' }),
    ]

    const { groups, ungrouped } = buildStrategyGroups(positions, undefined)

    expect(groups).toHaveLength(1)
    expect(ungrouped).toHaveLength(1)
    expect(ungrouped[0].instrumentId).toBe('MSFT')
  })

  it('handles empty positions array', () => {
    const { groups, ungrouped } = buildStrategyGroups([], undefined)

    expect(groups).toHaveLength(0)
    expect(ungrouped).toHaveLength(0)
  })

  it('handles undefined positionRisk gracefully', () => {
    const positions = [
      makePosition({ instrumentId: 'AAPL-CALL', strategyId: 'strat-1' }),
    ]

    expect(() => buildStrategyGroups(positions, undefined)).not.toThrow()
  })
})
