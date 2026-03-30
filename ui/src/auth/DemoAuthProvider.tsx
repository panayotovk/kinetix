import { useEffect, useState, type ReactNode } from 'react'
import { AuthContext, type AuthState } from './useAuth'
import { DEFAULT_PERSONA, type DemoPersona } from './demoPersonas'
import { DemoPersonaContext } from './useDemoPersona'
import { setDemoPersona } from './authFetch'

interface DemoAuthProviderProps {
  children: ReactNode
}

export function DemoAuthProvider({ children }: DemoAuthProviderProps) {
  const [persona, setPersona] = useState<DemoPersona>(DEFAULT_PERSONA)

  useEffect(() => {
    setDemoPersona(persona.username, persona.role)
  }, [persona])

  const authState: AuthState = {
    authenticated: true,
    initialising: false,
    token: null,
    username: persona.username,
    roles: [persona.role],
    logout: () => {},
  }

  return (
    <AuthContext.Provider value={authState}>
      <DemoPersonaContext.Provider value={{ persona, setPersona }}>
        {children}
      </DemoPersonaContext.Provider>
    </AuthContext.Provider>
  )
}
