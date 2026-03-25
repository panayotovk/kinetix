import { test, expect } from '@playwright/test'
import { mockAllApiRoutes } from './fixtures'

const STRATEGY_POSITIONS = [
  {
    bookId: 'port-1',
    instrumentId: 'AAPL-CALL',
    assetClass: 'EQUITY',
    instrumentType: 'EQUITY_OPTION',
    quantity: '10',
    averageCost: { amount: '5.00', currency: 'USD' },
    marketPrice: { amount: '8.00', currency: 'USD' },
    marketValue: { amount: '800.00', currency: 'USD' },
    unrealizedPnl: { amount: '300.00', currency: 'USD' },
    strategyId: 'strat-1',
    strategyType: 'STRADDLE',
    strategyName: 'Sep Straddle',
  },
  {
    bookId: 'port-1',
    instrumentId: 'AAPL-PUT',
    assetClass: 'EQUITY',
    instrumentType: 'EQUITY_OPTION',
    quantity: '10',
    averageCost: { amount: '4.00', currency: 'USD' },
    marketPrice: { amount: '6.00', currency: 'USD' },
    marketValue: { amount: '600.00', currency: 'USD' },
    unrealizedPnl: { amount: '200.00', currency: 'USD' },
    strategyId: 'strat-1',
    strategyType: 'STRADDLE',
    strategyName: 'Sep Straddle',
  },
  {
    bookId: 'port-1',
    instrumentId: 'MSFT',
    assetClass: 'EQUITY',
    instrumentType: 'CASH_EQUITY',
    quantity: '50',
    averageCost: { amount: '300.00', currency: 'USD' },
    marketPrice: { amount: '310.00', currency: 'USD' },
    marketValue: { amount: '15500.00', currency: 'USD' },
    unrealizedPnl: { amount: '500.00', currency: 'USD' },
  },
]

test.describe('Strategy Grouping', () => {
  test.beforeEach(async ({ page }) => {
    await mockAllApiRoutes(page)

    // Override positions to include strategy-grouped positions
    await page.route('**/api/v1/books/*/positions', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(STRATEGY_POSITIONS),
      })
    })
  })

  test('renders a strategy group row for positions with strategyId', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="strategy-row-strat-1"]')

    await expect(page.getByTestId('strategy-row-strat-1')).toBeVisible()
  })

  test('strategy group row shows strategy name and type badge', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="strategy-row-strat-1"]')

    const strategyRow = page.getByTestId('strategy-row-strat-1')
    await expect(strategyRow).toContainText('Sep Straddle')
    await expect(page.getByTestId('strategy-type-badge-strat-1')).toContainText('STRADDLE')
  })

  test('strategy group row shows net P&L', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="strategy-row-strat-1"]')

    // Net P&L = 300 + 200 = 500
    await expect(page.getByTestId('strategy-net-pnl-strat-1')).toBeVisible()
  })

  test('does not render individual position rows for strategy legs', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="strategy-row-strat-1"]')

    await expect(page.getByTestId('position-row-AAPL-CALL')).not.toBeVisible()
    await expect(page.getByTestId('position-row-AAPL-PUT')).not.toBeVisible()
  })

  test('ungrouped positions render as normal rows', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="position-row-MSFT"]')

    await expect(page.getByTestId('position-row-MSFT')).toBeVisible()
  })

  test('clicking a strategy row expands to show leg rows', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="strategy-row-strat-1"]')

    // Legs are hidden initially
    await expect(page.getByTestId('strategy-leg-AAPL-CALL')).not.toBeVisible()
    await expect(page.getByTestId('strategy-leg-AAPL-PUT')).not.toBeVisible()

    await page.getByTestId('strategy-row-strat-1').click()

    await expect(page.getByTestId('strategy-leg-AAPL-CALL')).toBeVisible()
    await expect(page.getByTestId('strategy-leg-AAPL-PUT')).toBeVisible()
  })

  test('leg rows show instrument ids when expanded', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="strategy-row-strat-1"]')

    await page.getByTestId('strategy-row-strat-1').click()

    await expect(page.getByTestId('strategy-leg-AAPL-CALL')).toContainText('AAPL-CALL')
    await expect(page.getByTestId('strategy-leg-AAPL-PUT')).toContainText('AAPL-PUT')
  })

  test('clicking a strategy row a second time collapses the legs', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="strategy-row-strat-1"]')

    await page.getByTestId('strategy-row-strat-1').click()
    await expect(page.getByTestId('strategy-leg-AAPL-CALL')).toBeVisible()

    await page.getByTestId('strategy-row-strat-1').click()
    await expect(page.getByTestId('strategy-leg-AAPL-CALL')).not.toBeVisible()
  })
})
