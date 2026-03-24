import { test, expect } from '@playwright/test'
import { mockAllApiRoutes } from './fixtures'
import type { Page, Route } from '@playwright/test'

async function navigateToExecutionCostTab(page: Page) {
  await mockAllApiRoutes(page)
  await page.goto('/')
  await page.getByTestId('tab-trades').click()
  await page.getByTestId('trades-subtab-cost').click()
}

test.describe('Execution Cost Panel', () => {
  test('shows empty state when no cost data exists for book', async ({ page }) => {
    await navigateToExecutionCostTab(page)
    // Empty list is mocked by default — select a book to trigger fetch
    await page.locator('[data-testid="book-selector"]').first().click().catch(() => {})
    await expect(page.getByText(/no execution cost data|select a book/i)).toBeVisible()
  })

  test('sub-tab navigation switches between Blotter, Execution Cost, and Reconciliation', async ({ page }) => {
    await mockAllApiRoutes(page)
    await page.goto('/')
    await page.getByTestId('tab-trades').click()

    // Blotter is default
    await expect(page.getByTestId('trades-subtab-blotter')).toHaveAttribute('aria-selected', 'true')

    // Switch to cost
    await page.getByTestId('trades-subtab-cost').click()
    await expect(page.getByTestId('trades-subtab-cost')).toHaveAttribute('aria-selected', 'true')

    // Switch to reconciliation
    await page.getByTestId('trades-subtab-reconciliation').click()
    await expect(page.getByTestId('trades-subtab-reconciliation')).toHaveAttribute('aria-selected', 'true')

    // Switch back to blotter
    await page.getByTestId('trades-subtab-blotter').click()
    await expect(page.getByTestId('trades-subtab-blotter')).toHaveAttribute('aria-selected', 'true')
  })

  test('shows execution cost table with slippage data', async ({ page }) => {
    await mockAllApiRoutes(page)

    // Override the execution cost route with real data
    await page.route('**/api/v1/execution/cost/**', (route: Route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            orderId: 'ord-e2e-001',
            bookId: 'port-1',
            instrumentId: 'AAPL',
            completedAt: '2026-03-24T15:00:00Z',
            arrivalPrice: '150.00',
            averageFillPrice: '150.15',
            side: 'BUY',
            totalQty: '100',
            slippageBps: '10.00',
            marketImpactBps: null,
            timingCostBps: null,
            totalCostBps: '10.00',
          },
        ]),
      })
    })

    await page.goto('/')
    await page.getByTestId('tab-trades').click()
    await page.getByTestId('trades-subtab-cost').click()

    await expect(page.getByTestId('cost-row-ord-e2e-001')).toBeVisible()
    await expect(page.getByText('AAPL')).toBeVisible()
    await expect(page.getByTestId('slippage-ord-e2e-001')).toBeVisible()
    await expect(page.getByTestId('slippage-ord-e2e-001')).toHaveClass(/text-amber-600/)
  })

  test('shows negative slippage in green for cost savings', async ({ page }) => {
    await mockAllApiRoutes(page)

    await page.route('**/api/v1/execution/cost/**', (route: Route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            orderId: 'ord-e2e-002',
            bookId: 'port-1',
            instrumentId: 'GOOGL',
            completedAt: '2026-03-24T15:00:00Z',
            arrivalPrice: '2850.00',
            averageFillPrice: '2848.00',
            side: 'BUY',
            totalQty: '50',
            slippageBps: '-7.02',
            marketImpactBps: null,
            timingCostBps: null,
            totalCostBps: '-7.02',
          },
        ]),
      })
    })

    await page.goto('/')
    await page.getByTestId('tab-trades').click()
    await page.getByTestId('trades-subtab-cost').click()

    await expect(page.getByTestId('slippage-ord-e2e-002')).toHaveClass(/text-green-600/)
  })
})
