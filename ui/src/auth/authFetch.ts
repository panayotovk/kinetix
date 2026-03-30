import { DEMO_MODE } from './demoPersonas'

/**
 * Module-level token ref, set by AuthProvider when the token changes.
 * API modules import authFetch instead of using global fetch directly.
 */
let currentToken: string | null = null

export function setAuthToken(token: string | null): void {
  currentToken = token
}

export function getAuthToken(): string | null {
  return currentToken
}

/**
 * Module-level persona ref for demo mode, set by DemoAuthProvider on persona switch.
 */
let demoUserId: string | null = null
let demoUserRole: string | null = null

export function setDemoPersona(userId: string | null, role: string | null): void {
  demoUserId = userId
  demoUserRole = role
}

/**
 * Wrapper around fetch that injects the Authorization header when a token is available.
 * In demo mode, injects X-Demo-User-Id and X-Demo-User-Role on mutating requests.
 * All API modules should use this instead of global fetch.
 *
 * When no token is set, passes through to fetch unchanged to avoid
 * altering header types (Headers vs plain object) in test environments.
 */
export async function authFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  if (!currentToken && !DEMO_MODE) {
    return init === undefined ? fetch(input) : fetch(input, init)
  }
  const headers = new Headers(init?.headers)
  if (currentToken) {
    headers.set('Authorization', `Bearer ${currentToken}`)
  }
  if (DEMO_MODE && demoUserId && demoUserRole) {
    const method = init?.method?.toUpperCase() ?? 'GET'
    if (method === 'POST' || method === 'PUT' || method === 'DELETE') {
      headers.set('X-Demo-User-Id', demoUserId)
      headers.set('X-Demo-User-Role', demoUserRole)
    }
  }
  return fetch(input, { ...init, headers })
}
