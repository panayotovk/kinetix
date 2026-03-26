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
 * Wrapper around fetch that injects the Authorization header when a token is available.
 * All API modules should use this instead of global fetch.
 *
 * When no token is set, passes through to fetch unchanged to avoid
 * altering header types (Headers vs plain object) in test environments.
 */
export async function authFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  if (!currentToken) {
    return init === undefined ? fetch(input) : fetch(input, init)
  }
  const headers = new Headers(init?.headers)
  headers.set('Authorization', `Bearer ${currentToken}`)
  return fetch(input, { ...init, headers })
}
