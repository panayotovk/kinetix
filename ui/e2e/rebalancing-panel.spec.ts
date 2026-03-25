import { test, expect } from '@playwright/test'
import { mockAllApiRoutes, mockRebalancingAnalysis, TEST_REBALANCING_RESPONSE } from './fixtures'

test.describe('Rebalancing Panel', () => {
  test.beforeEach(async ({ page }) => {
    await mockAllApiRoutes(page)
    await mockRebalancingAnalysis(page, TEST_REBALANCING_RESPONSE)
  })

  // ---------------------------------------------------------------------------
  // Mode toggle
  // ---------------------------------------------------------------------------

  test('switches to rebalancing mode via toggle', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="whatif-open-button"]')

    await page.getByTestId('whatif-open-button').click()
    await page.waitForSelector('[data-testid="whatif-mode-rebalancing"]')

    await page.getByTestId('whatif-mode-rebalancing').click()

    await expect(page.getByTestId('whatif-mode-rebalancing')).toHaveAttribute('aria-pressed', 'true')
    await expect(page.getByTestId('whatif-mode-whatif')).toHaveAttribute('aria-pressed', 'false')
  })

  test('shows bid-ask spread input after switching to rebalancing mode', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="whatif-open-button"]')

    await page.getByTestId('whatif-open-button').click()
    await page.waitForSelector('[data-testid="whatif-mode-rebalancing"]')

    await page.getByTestId('whatif-mode-rebalancing').click()

    await expect(page.getByTestId('whatif-bid-ask-0')).toBeVisible()
  })

  test('shows preset template buttons in rebalancing mode', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="whatif-open-button"]')

    await page.getByTestId('whatif-open-button').click()
    await page.waitForSelector('[data-testid="whatif-mode-rebalancing"]')

    await page.getByTestId('whatif-mode-rebalancing').click()

    await expect(page.getByTestId('whatif-preset-reduce-largest')).toBeVisible()
    await expect(page.getByTestId('whatif-preset-flatten-delta')).toBeVisible()
    await expect(page.getByTestId('whatif-preset-roll-expiring')).toBeVisible()
  })

  test('submit button label changes to "Run Rebalancing" in rebalancing mode', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="whatif-open-button"]')

    await page.getByTestId('whatif-open-button').click()
    await page.waitForSelector('[data-testid="whatif-mode-rebalancing"]')

    await page.getByTestId('whatif-mode-rebalancing').click()

    await expect(page.getByTestId('whatif-run')).toContainText('Run Rebalancing')
  })

  // ---------------------------------------------------------------------------
  // Rebalancing submission and result display
  // ---------------------------------------------------------------------------

  test('submits rebalancing and shows before/after comparison table', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="whatif-open-button"]')

    await page.getByTestId('whatif-open-button').click()
    await page.waitForSelector('[data-testid="whatif-instrument-0"]')

    // Switch to rebalancing mode
    await page.getByTestId('whatif-mode-rebalancing').click()

    // Fill trade details
    await page.getByTestId('whatif-instrument-0').fill('AAPL')
    await page.getByTestId('whatif-side-sell-0').click()
    await page.getByTestId('whatif-quantity-0').fill('50')
    await page.getByTestId('whatif-price-0').fill('170.00')

    // Submit
    await page.getByTestId('whatif-run').click()

    // Wait for rebalancing comparison to appear
    await page.waitForSelector('[data-testid="whatif-rebalancing-comparison"]')

    // Check before/after VaR
    await expect(page.getByTestId('whatif-rebal-var-base')).toHaveText('100,000.00')
    await expect(page.getByTestId('whatif-rebal-var-after')).toHaveText('80,000.00')
    await expect(page.getByTestId('whatif-rebal-var-change-pct')).toContainText('-20.00%')
  })

  test('shows per-trade contribution table after rebalancing', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="whatif-open-button"]')

    await page.getByTestId('whatif-open-button').click()
    await page.waitForSelector('[data-testid="whatif-instrument-0"]')

    await page.getByTestId('whatif-mode-rebalancing').click()

    await page.getByTestId('whatif-instrument-0').fill('AAPL')
    await page.getByTestId('whatif-side-sell-0').click()
    await page.getByTestId('whatif-quantity-0').fill('50')
    await page.getByTestId('whatif-price-0').fill('170.00')

    await page.getByTestId('whatif-run').click()

    await page.waitForSelector('[data-testid="whatif-trade-contributions"]')

    // AAPL contribution row
    await expect(page.getByTestId('whatif-contribution-instrument-0')).toHaveText('AAPL')
    await expect(page.getByTestId('whatif-contribution-var-impact-0')).toContainText('-12,000.00')
    await expect(page.getByTestId('whatif-contribution-cost-0')).toContainText('42.50')
  })

  test('shows estimated execution cost after rebalancing', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="whatif-open-button"]')

    await page.getByTestId('whatif-open-button').click()
    await page.waitForSelector('[data-testid="whatif-instrument-0"]')

    await page.getByTestId('whatif-mode-rebalancing').click()

    await page.getByTestId('whatif-instrument-0').fill('AAPL')
    await page.getByTestId('whatif-side-sell-0').click()
    await page.getByTestId('whatif-quantity-0').fill('50')
    await page.getByTestId('whatif-price-0').fill('170.00')

    await page.getByTestId('whatif-run').click()

    await page.waitForSelector('[data-testid="whatif-execution-cost"]')

    await expect(page.getByTestId('whatif-execution-cost')).toContainText('70.50')
  })

  test('resets rebalancing result when reset is clicked', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="whatif-open-button"]')

    await page.getByTestId('whatif-open-button').click()
    await page.waitForSelector('[data-testid="whatif-instrument-0"]')

    await page.getByTestId('whatif-mode-rebalancing').click()

    await page.getByTestId('whatif-instrument-0').fill('AAPL')
    await page.getByTestId('whatif-side-sell-0').click()
    await page.getByTestId('whatif-quantity-0').fill('50')
    await page.getByTestId('whatif-price-0').fill('170.00')

    await page.getByTestId('whatif-run').click()
    await page.waitForSelector('[data-testid="whatif-rebalancing-comparison"]')

    // Reset
    await page.getByTestId('whatif-reset').click()

    await expect(page.getByTestId('whatif-rebalancing-comparison')).not.toBeVisible()
    await expect(page.getByTestId('whatif-trade-contributions')).not.toBeVisible()
    await expect(page.getByTestId('whatif-reset')).not.toBeVisible()
  })

  test('adds a second trade in rebalancing mode', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="whatif-open-button"]')

    await page.getByTestId('whatif-open-button').click()
    await page.waitForSelector('[data-testid="whatif-mode-rebalancing"]')

    await page.getByTestId('whatif-mode-rebalancing').click()
    await page.getByTestId('whatif-add-trade').click()

    await expect(page.getByTestId('whatif-instrument-1')).toBeVisible()
    await expect(page.getByTestId('whatif-bid-ask-1')).toBeVisible()
  })
})
