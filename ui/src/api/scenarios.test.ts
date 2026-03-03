import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createScenario, listScenarios, listApprovedScenarios, submitScenario } from './scenarios'

describe('scenarios API', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('sends POST to create scenario and returns saved scenario', async () => {
    const saved = {
      id: 'scenario-1',
      name: 'My Scenario',
      description: 'Test',
      shocks: '{"volShocks":{"EQUITY":2.0},"priceShocks":{"EQUITY":0.8}}',
      status: 'DRAFT',
      createdBy: 'analyst@kinetix.com',
      approvedBy: null,
      approvedAt: null,
      createdAt: '2026-03-03T08:00:00Z',
    }
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(saved), { status: 201 }),
    )

    const result = await createScenario({
      name: 'My Scenario',
      description: 'Test',
      shocks: '{"volShocks":{"EQUITY":2.0},"priceShocks":{"EQUITY":0.8}}',
      createdBy: 'analyst@kinetix.com',
    })

    expect(result).toEqual(saved)
    expect(fetch).toHaveBeenCalledWith('/api/v1/stress-scenarios', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: 'My Scenario',
        description: 'Test',
        shocks: '{"volShocks":{"EQUITY":2.0},"priceShocks":{"EQUITY":0.8}}',
        createdBy: 'analyst@kinetix.com',
      }),
    })
  })

  it('throws on validation error (400)', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response('Bad Request', { status: 400, statusText: 'Bad Request' }),
    )

    await expect(
      createScenario({
        name: '',
        description: '',
        shocks: '{}',
        createdBy: '',
      }),
    ).rejects.toThrow('Failed to create scenario: 400 Bad Request')
  })

  it('lists all scenarios', async () => {
    const scenarios = [
      { id: '1', name: 'Scenario A', status: 'APPROVED' },
      { id: '2', name: 'Scenario B', status: 'DRAFT' },
    ]
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(scenarios), { status: 200 }),
    )

    const result = await listScenarios()

    expect(result).toEqual(scenarios)
    expect(fetch).toHaveBeenCalledWith('/api/v1/stress-scenarios')
  })

  it('lists approved scenarios', async () => {
    const scenarios = [{ id: '1', name: 'Scenario A', status: 'APPROVED' }]
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(scenarios), { status: 200 }),
    )

    const result = await listApprovedScenarios()

    expect(result).toEqual(scenarios)
    expect(fetch).toHaveBeenCalledWith('/api/v1/stress-scenarios/approved')
  })

  it('submits a scenario for approval', async () => {
    const updated = { id: '1', name: 'Scenario A', status: 'PENDING_APPROVAL' }
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(updated), { status: 200 }),
    )

    const result = await submitScenario('1')

    expect(result).toEqual(updated)
    expect(fetch).toHaveBeenCalledWith('/api/v1/stress-scenarios/1/submit', {
      method: 'PATCH',
    })
  })
})
