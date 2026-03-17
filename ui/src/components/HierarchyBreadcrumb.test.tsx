import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { HierarchyBreadcrumb } from './HierarchyBreadcrumb'
import type { BreadcrumbItem, HierarchySelection } from '../hooks/useHierarchySelector'

const firmCrumb: BreadcrumbItem = { level: 'firm', id: null, label: 'Firm' }
const divCrumb: BreadcrumbItem = { level: 'division', id: 'div-1', label: 'Equities' }
const deskCrumb: BreadcrumbItem = { level: 'desk', id: 'desk-1', label: 'EU Equities' }
const bookCrumb: BreadcrumbItem = { level: 'book', id: 'book-1', label: 'book-1' }

describe('HierarchyBreadcrumb', () => {
  it('renders Firm segment at firm level', () => {
    const onNavigate = vi.fn()
    render(<HierarchyBreadcrumb breadcrumb={[firmCrumb]} onNavigate={onNavigate} />)

    expect(screen.getByTestId('breadcrumb-firm')).toHaveTextContent('Firm')
  })

  it('renders all segments at book level', () => {
    const onNavigate = vi.fn()
    render(
      <HierarchyBreadcrumb
        breadcrumb={[firmCrumb, divCrumb, deskCrumb, bookCrumb]}
        onNavigate={onNavigate}
      />,
    )

    expect(screen.getByTestId('breadcrumb-firm')).toHaveTextContent('Firm')
    expect(screen.getByTestId('breadcrumb-division')).toHaveTextContent('Equities')
    expect(screen.getByTestId('breadcrumb-desk')).toHaveTextContent('EU Equities')
    expect(screen.getByTestId('breadcrumb-book')).toHaveTextContent('book-1')
  })

  it('last segment has aria-current="page"', () => {
    const onNavigate = vi.fn()
    render(
      <HierarchyBreadcrumb
        breadcrumb={[firmCrumb, divCrumb]}
        onNavigate={onNavigate}
      />,
    )

    expect(screen.getByTestId('breadcrumb-division')).toHaveAttribute('aria-current', 'page')
    expect(screen.getByTestId('breadcrumb-firm')).not.toHaveAttribute('aria-current')
  })

  it('calls onNavigate with firm selection when Firm clicked', () => {
    const onNavigate = vi.fn()
    render(
      <HierarchyBreadcrumb
        breadcrumb={[firmCrumb, divCrumb, deskCrumb]}
        onNavigate={onNavigate}
      />,
    )

    fireEvent.click(screen.getByTestId('breadcrumb-firm'))

    const call = onNavigate.mock.calls[0][0] as HierarchySelection
    expect(call.level).toBe('firm')
    expect(call.divisionId).toBeNull()
    expect(call.deskId).toBeNull()
  })

  it('calls onNavigate with division selection when division segment clicked', () => {
    const onNavigate = vi.fn()
    render(
      <HierarchyBreadcrumb
        breadcrumb={[firmCrumb, divCrumb, deskCrumb]}
        onNavigate={onNavigate}
      />,
    )

    fireEvent.click(screen.getByTestId('breadcrumb-division'))

    const call = onNavigate.mock.calls[0][0] as HierarchySelection
    expect(call.level).toBe('division')
    expect(call.divisionId).toBe('div-1')
    expect(call.deskId).toBeNull()
  })

  it('does not call onNavigate when last segment is clicked', () => {
    const onNavigate = vi.fn()
    render(
      <HierarchyBreadcrumb
        breadcrumb={[firmCrumb, divCrumb]}
        onNavigate={onNavigate}
      />,
    )

    fireEvent.click(screen.getByTestId('breadcrumb-division'))

    expect(onNavigate).not.toHaveBeenCalled()
  })

  it('last segment button is disabled', () => {
    const onNavigate = vi.fn()
    render(
      <HierarchyBreadcrumb
        breadcrumb={[firmCrumb, divCrumb, deskCrumb]}
        onNavigate={onNavigate}
      />,
    )

    expect(screen.getByTestId('breadcrumb-desk')).toBeDisabled()
    expect(screen.getByTestId('breadcrumb-firm')).not.toBeDisabled()
    expect(screen.getByTestId('breadcrumb-division')).not.toBeDisabled()
  })
})
