import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { DemoWelcomeStrip } from './DemoWelcomeStrip'

vi.mock('../auth/demoPersonas', () => ({
  DEMO_MODE: true,
}))

describe('DemoWelcomeStrip', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('renders when localStorage flag is absent', () => {
    render(<DemoWelcomeStrip />)
    expect(screen.getByTestId('demo-welcome-strip')).toBeInTheDocument()
  })

  it('does not render when localStorage flag is set', () => {
    localStorage.setItem('kinetix_demo_strip_dismissed', 'true')
    render(<DemoWelcomeStrip />)
    expect(screen.queryByTestId('demo-welcome-strip')).not.toBeInTheDocument()
  })

  it('clicking dismiss removes the strip from DOM', () => {
    render(<DemoWelcomeStrip />)
    expect(screen.getByTestId('demo-welcome-strip')).toBeInTheDocument()

    fireEvent.click(screen.getByTestId('demo-strip-dismiss'))
    expect(screen.queryByTestId('demo-welcome-strip')).not.toBeInTheDocument()
  })

  it('clicking dismiss writes persistence flag to localStorage', () => {
    render(<DemoWelcomeStrip />)
    fireEvent.click(screen.getByTestId('demo-strip-dismiss'))
    expect(localStorage.getItem('kinetix_demo_strip_dismissed')).toBe('true')
  })

  it('strip does not render after dismiss and re-mount', () => {
    const { unmount } = render(<DemoWelcomeStrip />)
    fireEvent.click(screen.getByTestId('demo-strip-dismiss'))
    unmount()

    render(<DemoWelcomeStrip />)
    expect(screen.queryByTestId('demo-welcome-strip')).not.toBeInTheDocument()
  })

  it('strip copy contains expected text', () => {
    render(<DemoWelcomeStrip />)
    expect(screen.getByTestId('demo-welcome-strip')).toHaveTextContent('Demo mode')
    expect(screen.getByTestId('demo-welcome-strip')).toHaveTextContent('Switch personas')
  })

  it('dismiss button has accessible label', () => {
    render(<DemoWelcomeStrip />)
    expect(screen.getByRole('button', { name: /dismiss/i })).toBeInTheDocument()
  })
})
