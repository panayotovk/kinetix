import type { CreateScenarioRequestDto, StressScenarioDto } from '../types'

export async function createScenario(
  request: CreateScenarioRequestDto,
): Promise<StressScenarioDto> {
  const response = await fetch('/api/v1/stress-scenarios', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) {
    throw new Error(
      `Failed to create scenario: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}

export async function listScenarios(): Promise<StressScenarioDto[]> {
  const response = await fetch('/api/v1/stress-scenarios')
  if (!response.ok) {
    throw new Error(
      `Failed to list scenarios: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}

export async function listApprovedScenarios(): Promise<StressScenarioDto[]> {
  const response = await fetch('/api/v1/stress-scenarios/approved')
  if (!response.ok) {
    throw new Error(
      `Failed to list approved scenarios: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}

export async function submitScenario(id: string): Promise<StressScenarioDto> {
  const response = await fetch(`/api/v1/stress-scenarios/${id}/submit`, {
    method: 'PATCH',
  })
  if (!response.ok) {
    throw new Error(
      `Failed to submit scenario: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}
