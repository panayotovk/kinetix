import { createContext, useContext } from 'react'

export interface AuthState {
  authenticated: boolean
  initialising: boolean
  token: string | null
  username: string | null
  roles: string[]
  logout: () => void
}

export const AuthContext = createContext<AuthState>({
  authenticated: false,
  initialising: true,
  token: null,
  username: null,
  roles: [],
  logout: () => {},
})

export function useAuth(): AuthState {
  return useContext(AuthContext)
}
