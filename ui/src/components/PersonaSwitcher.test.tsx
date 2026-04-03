import { describe, it, expect } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PersonaSwitcher } from './PersonaSwitcher'
import { DemoAuthProvider } from '../auth/DemoAuthProvider'
import { DEMO_PERSONAS } from '../auth/demoPersonas'

function renderSwitcher() {
  return render(
    <DemoAuthProvider>
      <PersonaSwitcher />
    </DemoAuthProvider>,
  )
}

describe('PersonaSwitcher', () => {
  it('renders toggle with default RISK_MANAGER persona', () => {
    renderSwitcher()
    expect(screen.getByTestId('header-role-badge')).toHaveTextContent('RISK MANAGER')
    expect(screen.getByTestId('header-username')).toHaveTextContent('risk_manager1')
  })

  it('toggle has visible button shape with border styling', () => {
    renderSwitcher()
    const toggle = screen.getByTestId('persona-switcher-toggle')
    expect(toggle.className).toContain('border')
    expect(toggle.className).toContain('rounded-md')
  })

  it('opens dropdown on click showing all 5 personas', () => {
    renderSwitcher()
    expect(screen.queryByTestId('persona-switcher-panel')).not.toBeInTheDocument()

    fireEvent.click(screen.getByTestId('persona-switcher-toggle'))

    expect(screen.getByTestId('persona-switcher-panel')).toBeInTheDocument()
    for (const p of DEMO_PERSONAS) {
      expect(screen.getByTestId(`persona-option-${p.key}`)).toBeInTheDocument()
    }
  })

  it('switches persona when option clicked', () => {
    renderSwitcher()
    fireEvent.click(screen.getByTestId('persona-switcher-toggle'))
    fireEvent.click(screen.getByTestId('persona-option-trader'))

    expect(screen.getByTestId('header-role-badge')).toHaveTextContent('TRADER')
    expect(screen.getByTestId('header-username')).toHaveTextContent('trader1')
  })

  it('closes dropdown after selection', () => {
    renderSwitcher()
    fireEvent.click(screen.getByTestId('persona-switcher-toggle'))
    expect(screen.getByTestId('persona-switcher-panel')).toBeInTheDocument()

    fireEvent.click(screen.getByTestId('persona-option-admin'))
    expect(screen.queryByTestId('persona-switcher-panel')).not.toBeInTheDocument()
  })

  it('closes dropdown on click outside', () => {
    renderSwitcher()
    fireEvent.click(screen.getByTestId('persona-switcher-toggle'))
    expect(screen.getByTestId('persona-switcher-panel')).toBeInTheDocument()

    fireEvent.mouseDown(document.body)
    expect(screen.queryByTestId('persona-switcher-panel')).not.toBeInTheDocument()
  })

  it('highlights active persona with check mark and others without', () => {
    renderSwitcher()
    fireEvent.click(screen.getByTestId('persona-switcher-toggle'))

    const activeOption = screen.getByTestId('persona-option-risk_manager')
    expect(activeOption.getAttribute('aria-selected')).toBe('true')

    const inactiveOption = screen.getByTestId('persona-option-trader')
    expect(inactiveOption.getAttribute('aria-selected')).toBe('false')
  })

  it('keyboard: ArrowDown/ArrowUp moves focus, Enter selects, Escape closes', () => {
    renderSwitcher()
    const toggle = screen.getByTestId('persona-switcher-toggle')

    // ArrowDown opens dropdown and focuses first option
    fireEvent.keyDown(toggle, { key: 'ArrowDown' })
    expect(screen.getByTestId('persona-switcher-panel')).toBeInTheDocument()

    const firstOption = screen.getByTestId('persona-option-risk_manager')
    // ArrowDown moves to next option
    fireEvent.keyDown(firstOption, { key: 'ArrowDown' })
    const secondOption = screen.getByTestId('persona-option-trader')
    expect(document.activeElement).toBe(secondOption)

    // Enter selects the focused option
    fireEvent.keyDown(secondOption, { key: 'Enter' })
    expect(screen.getByTestId('header-role-badge')).toHaveTextContent('TRADER')
    expect(screen.queryByTestId('persona-switcher-panel')).not.toBeInTheDocument()
  })

  it('focus returns to toggle button after selection', () => {
    renderSwitcher()
    const toggle = screen.getByTestId('persona-switcher-toggle')
    fireEvent.click(toggle)
    fireEvent.click(screen.getByTestId('persona-option-compliance'))

    expect(document.activeElement).toBe(toggle)
  })

  it('has correct ARIA attributes', () => {
    renderSwitcher()
    const toggle = screen.getByTestId('persona-switcher-toggle')
    expect(toggle.getAttribute('aria-haspopup')).toBe('listbox')
    expect(toggle.getAttribute('aria-expanded')).toBe('false')

    fireEvent.click(toggle)
    expect(toggle.getAttribute('aria-expanded')).toBe('true')

    const panel = screen.getByTestId('persona-switcher-panel')
    expect(panel.getAttribute('role')).toBe('listbox')

    const option = screen.getByTestId('persona-option-risk_manager')
    expect(option.getAttribute('role')).toBe('option')
  })
})
