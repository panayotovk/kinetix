import { describe, it, expect, vi, beforeEach } from 'vitest'
import { runHistoricalReplay, runReverseStress } from './historicalReplay'

const mockReplayResult = {
  scenarioName: 'GFC_2008',
  totalPnlImpact: '-125000.00',
  positionImpacts: [
    {
      instrumentId: 'AAPL',
      assetClass: 'EQUITY',
      marketValue: '15500.00',
      pnlImpact: '-125000.00',
      dailyPnl: ['-50000.00', '-25000.00', '10000.00', '-40000.00', '-20000.00'],
      proxyUsed: false,
    },
  ],
  windowStart: '2008-09-15',
  windowEnd: '2008-09-19',
  calculatedAt: '2025-01-15T10:00:00Z',
}

const mockReverseResult = {
  shocks: [{ instrumentId: 'AAPL', shock: '-0.100000' }],
  achievedLoss: '100000.00',
  targetLoss: '100000.00',
  converged: true,
  calculatedAt: '2025-01-15T10:00:00Z',
}

describe('runHistoricalReplay', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  it('calls the correct endpoint with POST and returns result', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockReplayResult),
    } as Response)

    const result = await runHistoricalReplay('port-1', {
      instrumentReturns: [],
      windowStart: '2008-09-15',
      windowEnd: '2008-09-19',
    })

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/risk/stress/port-1/historical-replay',
      expect.objectContaining({ method: 'POST' }),
    )
    expect(result.scenarioName).toBe('GFC_2008')
    expect(result.totalPnlImpact).toBe('-125000.00')
  })

  it('sends instrument returns in the request body', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockReplayResult),
    } as Response)

    await runHistoricalReplay('port-1', {
      instrumentReturns: [{ instrumentId: 'AAPL', dailyReturns: [-0.05, -0.03, 0.01] }],
    })

    const [, init] = vi.mocked(fetch).mock.calls[0]
    const body = JSON.parse((init as RequestInit).body as string)
    expect(body.instrumentReturns[0].instrumentId).toBe('AAPL')
    expect(body.instrumentReturns[0].dailyReturns).toEqual([-0.05, -0.03, 0.01])
  })

  it('throws on non-OK response', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
    } as Response)

    await expect(runHistoricalReplay('port-1', { instrumentReturns: [] })).rejects.toThrow(
      'Historical replay failed',
    )
  })
})

describe('runReverseStress', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  it('calls the correct endpoint with POST and returns result', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockReverseResult),
    } as Response)

    const result = await runReverseStress('port-1', { targetLoss: 100000, maxShock: -1.0 })

    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/risk/stress/port-1/reverse',
      expect.objectContaining({ method: 'POST' }),
    )
    expect(result.converged).toBe(true)
    expect(result.shocks[0].instrumentId).toBe('AAPL')
  })

  it('throws on non-OK response', async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: false,
      status: 400,
      statusText: 'Bad Request',
    } as Response)

    await expect(runReverseStress('port-1', { targetLoss: 0 })).rejects.toThrow(
      'Reverse stress failed',
    )
  })
})
