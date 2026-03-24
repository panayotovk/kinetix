import { test, expect } from '@playwright/test'
import {
  mockAllApiRoutes,
  mockFactorRiskRoutes,
  TEST_FACTOR_RISK_RESULT,
  TEST_FACTOR_RISK_CONCENTRATION_WARNING,
} from './fixtures'

async function goToRiskTab(page: import('@playwright/test').Page) {
  await page.goto('/')
  await page.getByTestId('tab-risk').click()
}

// ---------------------------------------------------------------------------
// Factor Decomposition Panel — empty state
// ---------------------------------------------------------------------------

test.describe('Factor Decomposition Panel — empty state', () => {
  test('shows empty state when no factor snapshot exists', async ({ page }) => {
    await mockAllApiRoutes(page)
    await mockFactorRiskRoutes(page, { latest: null })

    await goToRiskTab(page)
    await page.waitForSelector('[data-testid="factor-risk-empty"]')

    await expect(page.getByTestId('factor-risk-empty')).toBeVisible()
  })
})

// ---------------------------------------------------------------------------
// Factor Decomposition Panel — data rendering
// ---------------------------------------------------------------------------

test.describe('Factor Decomposition Panel — data rendering', () => {
  test.beforeEach(async ({ page }) => {
    await mockAllApiRoutes(page)
    await mockFactorRiskRoutes(page, { latest: TEST_FACTOR_RISK_RESULT })
  })

  test('displays total VaR when snapshot is available', async ({ page }) => {
    await goToRiskTab(page)
    await page.waitForSelector('[data-testid="factor-total-var"]')

    await expect(page.getByTestId('factor-total-var')).toContainText('50')
  })

  test('displays systematic and idiosyncratic VaR', async ({ page }) => {
    await goToRiskTab(page)
    await page.waitForSelector('[data-testid="factor-systematic-var"]')

    await expect(page.getByTestId('factor-systematic-var')).toContainText('38')
    await expect(page.getByTestId('factor-idiosyncratic-var')).toContainText('12')
  })

  test('displays R-squared as a percentage', async ({ page }) => {
    await goToRiskTab(page)
    await page.waitForSelector('[data-testid="factor-r-squared"]')

    await expect(page.getByTestId('factor-r-squared')).toContainText('57.6')
  })

  test('renders a table row for each factor', async ({ page }) => {
    await goToRiskTab(page)
    await page.waitForSelector('[data-testid="factor-row-EQUITY_BETA"]')

    await expect(page.getByTestId('factor-row-EQUITY_BETA')).toBeVisible()
    await expect(page.getByTestId('factor-row-RATES_DURATION')).toBeVisible()
    await expect(page.getByTestId('factor-row-CREDIT_SPREAD')).toBeVisible()
    await expect(page.getByTestId('factor-row-FX_DELTA')).toBeVisible()
    await expect(page.getByTestId('factor-row-VOL_EXPOSURE')).toBeVisible()
  })

  test('renders the stacked bar', async ({ page }) => {
    await goToRiskTab(page)
    await page.waitForSelector('[data-testid="factor-stacked-bar"]')

    await expect(page.getByTestId('factor-stacked-bar')).toBeVisible()
    await expect(page.getByTestId('factor-bar-EQUITY_BETA')).toBeVisible()
  })

  test('does not show concentration warning when flag is false', async ({ page }) => {
    await goToRiskTab(page)
    await page.waitForSelector('[data-testid="factor-total-var"]')

    await expect(page.getByTestId('concentration-warning')).not.toBeVisible()
  })
})

// ---------------------------------------------------------------------------
// Factor Decomposition Panel — concentration warning
// ---------------------------------------------------------------------------

test.describe('Factor Decomposition Panel — concentration warning', () => {
  test('shows concentration warning badge when a single factor dominates', async ({ page }) => {
    await mockAllApiRoutes(page)
    await mockFactorRiskRoutes(page, { latest: TEST_FACTOR_RISK_CONCENTRATION_WARNING })

    await goToRiskTab(page)
    await page.waitForSelector('[data-testid="concentration-warning"]')

    await expect(page.getByTestId('concentration-warning')).toBeVisible()
    await expect(page.getByTestId('concentration-warning')).toContainText('Concentration Warning')
  })
})
