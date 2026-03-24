import { test, expect } from '@playwright/test'
import { mockAllApiRoutes } from './fixtures'
import type { Page, Route } from '@playwright/test'

async function navigateToReconciliationTab(page: Page) {
  await mockAllApiRoutes(page)
  await page.goto('/')
  await page.getByTestId('tab-trades').click()
  await page.getByTestId('trades-subtab-reconciliation').click()
}

test.describe('Reconciliation Panel', () => {
  test('shows empty state when no reconciliation data exists', async ({ page }) => {
    await navigateToReconciliationTab(page)
    await expect(page.getByText(/no reconciliation data|select a book/i)).toBeVisible()
  })

  test('shows clean reconciliation with CLEAN status badge', async ({ page }) => {
    await mockAllApiRoutes(page)

    await page.route('**/api/v1/execution/reconciliation/**', (route: Route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            reconciliationDate: '2026-03-24',
            bookId: 'port-1',
            status: 'CLEAN',
            totalPositions: 3,
            matchedCount: 3,
            breakCount: 0,
            breaks: [],
            reconciledAt: '2026-03-24T18:00:00Z',
          },
        ]),
      })
    })

    await page.goto('/')
    await page.getByTestId('tab-trades').click()
    await page.getByTestId('trades-subtab-reconciliation').click()

    await expect(page.getByTestId('recon-row-2026-03-24')).toBeVisible()
    await expect(page.getByText('CLEAN')).toBeVisible()
    await expect(page.getByText('3 / 3')).toBeVisible()
  })

  test('shows amber highlighting for reconciliation with breaks', async ({ page }) => {
    await mockAllApiRoutes(page)

    await page.route('**/api/v1/execution/reconciliation/**', (route: Route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            reconciliationDate: '2026-03-23',
            bookId: 'port-1',
            status: 'BREAKS_FOUND',
            totalPositions: 3,
            matchedCount: 2,
            breakCount: 1,
            breaks: [
              {
                instrumentId: 'AAPL',
                internalQty: '105',
                primeBrokerQty: '100',
                breakQty: '5',
                breakNotional: '750.00',
              },
            ],
            reconciledAt: '2026-03-23T18:00:00Z',
          },
        ]),
      })
    })

    await page.goto('/')
    await page.getByTestId('tab-trades').click()
    await page.getByTestId('trades-subtab-reconciliation').click()

    const breakRow = page.getByTestId('recon-row-2026-03-23')
    await expect(breakRow).toBeVisible()
    await expect(breakRow).toHaveClass(/bg-amber-50/)
    await expect(page.getByText('BREAKS_FOUND')).toBeVisible()
  })

  test('shows break instrument detail rows highlighted amber', async ({ page }) => {
    await mockAllApiRoutes(page)

    await page.route('**/api/v1/execution/reconciliation/**', (route: Route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            reconciliationDate: '2026-03-23',
            bookId: 'port-1',
            status: 'BREAKS_FOUND',
            totalPositions: 2,
            matchedCount: 1,
            breakCount: 1,
            breaks: [
              {
                instrumentId: 'MSFT',
                internalQty: '200',
                primeBrokerQty: '195',
                breakQty: '5',
                breakNotional: '1850.00',
              },
            ],
            reconciledAt: '2026-03-23T18:00:00Z',
          },
        ]),
      })
    })

    await page.goto('/')
    await page.getByTestId('tab-trades').click()
    await page.getByTestId('trades-subtab-reconciliation').click()

    const instrumentBreakRow = page.getByTestId('break-row-MSFT')
    await expect(instrumentBreakRow).toBeVisible()
    await expect(instrumentBreakRow).toHaveClass(/bg-amber-50/)
    await expect(page.getByText('1850.00')).toBeVisible()
  })
})
