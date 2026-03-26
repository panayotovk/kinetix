import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './useAuth'

// Mock keycloak-js
const mockKeycloak = {
  init: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  token: 'mock-access-token',
  tokenParsed: {
    preferred_username: 'trader1',
    roles: ['TRADER'],
    sub: 'user-uuid-1',
  },
  updateToken: vi.fn(),
}

function MockKeycloak() {
  return mockKeycloak
}

vi.mock('keycloak-js', () => ({
  default: MockKeycloak,
}))

function AuthConsumer() {
  const auth = useAuth()
  return (
    <div>
      <span data-testid="username">{auth.username}</span>
      <span data-testid="roles">{auth.roles.join(',')}</span>
      <span data-testid="authenticated">{String(auth.authenticated)}</span>
      <button data-testid="logout" onClick={auth.logout}>Logout</button>
    </div>
  )
}

describe('AuthProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockKeycloak.init.mockResolvedValue(true)
    mockKeycloak.updateToken.mockResolvedValue(false)
  })

  it('renders children when authenticated', async () => {
    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    )

    await screen.findByTestId('authenticated')
    expect(screen.getByTestId('authenticated').textContent).toBe('true')
  })

  it('provides username from Keycloak token', async () => {
    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    )

    await screen.findByTestId('username')
    expect(screen.getByTestId('username').textContent).toBe('trader1')
  })

  it('provides roles from Keycloak token', async () => {
    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    )

    await screen.findByTestId('roles')
    expect(screen.getByTestId('roles').textContent).toBe('TRADER')
  })

  it('shows loading screen during initialisation', () => {
    mockKeycloak.init.mockReturnValue(new Promise(() => {})) // Never resolves
    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    )

    expect(screen.getByText('Authenticating...')).toBeInTheDocument()
    expect(screen.queryByTestId('username')).not.toBeInTheDocument()
  })

  it('calls keycloak.logout with redirectUri on logout', async () => {
    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    )

    await screen.findByTestId('logout')
    screen.getByTestId('logout').click()
    expect(mockKeycloak.logout).toHaveBeenCalledWith({
      redirectUri: window.location.origin,
    })
  })
})
