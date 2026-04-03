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

  test('renders multiple rows with mixed positive and negative slippage', async ({ page }) => {
    await mockAllApiRoutes(page)

    await page.route('**/api/v1/execution/cost/**', (route: Route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            orderId: 'ord-mix-001',
            bookId: 'port-1',
            instrumentId: 'AAPL',
            completedAt: '2026-02-21T14:03:00Z',
            arrivalPrice: '185.25',
            averageFillPrice: '185.50',
            side: 'BUY',
            totalQty: '150',
            slippageBps: '13.4953',
            marketImpactBps: '3.5000',
            timingCostBps: '1.2000',
            totalCostBps: '18.1953',
          },
          {
            orderId: 'ord-mix-002',
            bookId: 'port-1',
            instrumentId: 'GOOGL',
            completedAt: '2026-02-21T14:07:00Z',
            arrivalPrice: '175.35',
            averageFillPrice: '175.20',
            side: 'BUY',
            totalQty: '80',
            slippageBps: '-8.5530',
            marketImpactBps: null,
            timingCostBps: null,
            totalCostBps: '-8.5530',
          },
          {
            orderId: 'ord-mix-003',
            bookId: 'port-1',
            instrumentId: 'MSFT',
            completedAt: '2026-02-21T14:12:00Z',
            arrivalPrice: '419.70',
            averageFillPrice: '420.00',
            side: 'BUY',
            totalQty: '120',
            slippageBps: '7.1480',
            marketImpactBps: '2.8000',
            timingCostBps: null,
            totalCostBps: '9.9480',
          },
          {
            orderId: 'ord-mix-004',
            bookId: 'port-1',
            instrumentId: 'BABA',
            completedAt: '2026-02-25T14:11:00Z',
            arrivalPrice: '86.70',
            averageFillPrice: '86.50',
            side: 'SELL',
            totalQty: '100',
            slippageBps: '23.0681',
            marketImpactBps: null,
            timingCostBps: null,
            totalCostBps: '23.0681',
          },
        ]),
      })
    })

    await page.goto('/')
    await page.getByTestId('tab-trades').click()
    await page.getByTestId('trades-subtab-cost').click()

    // All 4 rows rendered
    await expect(page.getByTestId('cost-row-ord-mix-001')).toBeVisible()
    await expect(page.getByTestId('cost-row-ord-mix-002')).toBeVisible()
    await expect(page.getByTestId('cost-row-ord-mix-003')).toBeVisible()
    await expect(page.getByTestId('cost-row-ord-mix-004')).toBeVisible()

    // Positive slippage rows have amber colour
    await expect(page.getByTestId('slippage-ord-mix-001')).toHaveClass(/text-amber-600/)
    await expect(page.getByTestId('slippage-ord-mix-003')).toHaveClass(/text-amber-600/)
    await expect(page.getByTestId('slippage-ord-mix-004')).toHaveClass(/text-amber-600/)

    // Negative slippage row has green colour
    await expect(page.getByTestId('slippage-ord-mix-002')).toHaveClass(/text-green-600/)

    // SELL side displayed in red, BUY in green
    await expect(page.getByTestId('side-ord-mix-001')).toHaveText('BUY')
    await expect(page.getByTestId('side-ord-mix-001')).toHaveClass(/text-green-600/)
    await expect(page.getByTestId('side-ord-mix-004')).toHaveText('SELL')
    await expect(page.getByTestId('side-ord-mix-004')).toHaveClass(/text-red-600/)
  })

  test('shows totalCostBps distinct from slippageBps when market impact is present', async ({ page }) => {
    await mockAllApiRoutes(page)

    await page.route('**/api/v1/execution/cost/**', (route: Route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            orderId: 'ord-impact-001',
            bookId: 'port-1',
            instrumentId: 'NVDA',
            completedAt: '2026-02-23T14:09:00Z',
            arrivalPrice: '883.50',
            averageFillPrice: '885.00',
            side: 'BUY',
            totalQty: '90',
            slippageBps: '16.9836',
            marketImpactBps: '5.0000',
            timingCostBps: '2.5000',
            totalCostBps: '24.4836',
          },
        ]),
      })
    })

    await page.goto('/')
    await page.getByTestId('tab-trades').click()
    await page.getByTestId('trades-subtab-cost').click()

    const row = page.getByTestId('cost-row-ord-impact-001')
    await expect(row).toBeVisible()

    // Slippage and total cost columns show different values
    await expect(page.getByTestId('slippage-ord-impact-001')).toContainText('16.98')
    // Total cost includes market impact + timing cost so is higher
    await expect(row.locator('td').nth(7)).toContainText('24.48')
  })
})
