import { describe, it, expect } from 'vitest'
import { buildStressResultsCsv } from './exportStressResults'
import { makeStressResult } from '../test-utils/stressMocks'

describe('exportStressResults', () => {
  it('should produce CSV with scenario summary rows', () => {
    const results = [
      makeStressResult({ scenarioName: 'GFC_2008', baseVar: '100000.00', stressedVar: '300000.00', pnlImpact: '-500000.00' }),
      makeStressResult({ scenarioName: 'COVID_2020', baseVar: '100000.00', stressedVar: '250000.00', pnlImpact: '-350000.00' }),
    ]

    const csv = buildStressResultsCsv(results)
    const lines = csv.split('\n')

    expect(lines[0]).toBe('Scenario,Base VaR,Stressed VaR,VaR Multiplier,P&L Impact,Calculated At')
    expect(lines[1]).toContain('GFC_2008')
    expect(lines[1]).toContain('100000.00')
    expect(lines[1]).toContain('300000.00')
    expect(lines[1]).toContain('3.00x')
    expect(lines[1]).toContain('-500000.00')
    expect(lines[2]).toContain('COVID_2020')
  })

  it('should include position detail rows', () => {
    const results = [
      makeStressResult({
        scenarioName: 'GFC_2008',
        positionImpacts: [
          {
            instrumentId: 'AAPL',
            assetClass: 'EQUITY',
            baseMarketValue: '500000.00',
            stressedMarketValue: '300000.00',
            pnlImpact: '-200000.00',
            percentageOfTotal: '40.00',
          },
        ],
      }),
    ]

    const csv = buildStressResultsCsv(results)

    // Position detail section
    expect(csv).toContain('Position Details')
    expect(csv).toContain('AAPL')
    expect(csv).toContain('EQUITY')
    expect(csv).toContain('500000.00')
    expect(csv).toContain('300000.00')
    expect(csv).toContain('-200000.00')
    expect(csv).toContain('40.00')
  })

  it('should handle empty results', () => {
    const csv = buildStressResultsCsv([])
    const lines = csv.split('\n')

    // Should still have the header
    expect(lines[0]).toBe('Scenario,Base VaR,Stressed VaR,VaR Multiplier,P&L Impact,Calculated At')
    expect(lines.length).toBe(1)
  })
})
