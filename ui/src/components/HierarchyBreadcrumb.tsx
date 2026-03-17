import { ChevronRight } from 'lucide-react'
import type { BreadcrumbItem, HierarchySelection, SelectionLevel } from '../hooks/useHierarchySelector'

interface HierarchyBreadcrumbProps {
  breadcrumb: BreadcrumbItem[]
  onNavigate: (selection: HierarchySelection) => void
}

export function HierarchyBreadcrumb({ breadcrumb, onNavigate }: HierarchyBreadcrumbProps) {
  const handleClick = (index: number, item: BreadcrumbItem) => {
    // Build selection for navigating to this breadcrumb item
    const divisionId = breadcrumb.find((b) => b.level === 'division')?.id ?? null
    const deskId = breadcrumb.find((b) => b.level === 'desk')?.id ?? null
    const bookId = breadcrumb.find((b) => b.level === 'book')?.id ?? null

    const level: SelectionLevel = item.level
    let newDivisionId: string | null = null
    let newDeskId: string | null = null
    let newBookId: string | null = null

    switch (level) {
      case 'firm':
        break
      case 'division':
        newDivisionId = divisionId
        break
      case 'desk':
        newDivisionId = divisionId
        newDeskId = deskId
        break
      case 'book':
        newDivisionId = divisionId
        newDeskId = deskId
        newBookId = bookId
        break
    }

    // If clicking current (last) item, do nothing
    if (index === breadcrumb.length - 1) return

    onNavigate({
      level,
      divisionId: newDivisionId,
      deskId: newDeskId,
      bookId: newBookId,
    })
  }

  return (
    <nav
      data-testid="hierarchy-breadcrumb"
      aria-label="Hierarchy navigation"
      className="flex items-center gap-1 text-sm"
    >
      {breadcrumb.map((item, index) => {
        const isLast = index === breadcrumb.length - 1
        return (
          <span key={`${item.level}-${item.id ?? 'root'}`} className="flex items-center gap-1">
            {index > 0 && (
              <ChevronRight className="h-3 w-3 text-slate-400 flex-shrink-0" aria-hidden="true" />
            )}
            <button
              data-testid={`breadcrumb-${item.level}`}
              onClick={() => handleClick(index, item)}
              disabled={isLast}
              className={`${
                isLast
                  ? 'text-slate-800 font-medium cursor-default'
                  : 'text-primary-600 hover:text-primary-800 hover:underline cursor-pointer'
              } transition-colors`}
              aria-current={isLast ? 'page' : undefined}
            >
              {item.label}
            </button>
          </span>
        )
      })}
    </nav>
  )
}
