import { test, expect, type Page } from '@playwright/test'
import { mockAllApiRoutes, mockHedgeSuggest, TEST_HEDGE_RECOMMENDATION } from './fixtures'

/**
 * Navigates to the Risk tab for a book so the Suggest Hedge button is visible.
 * Mirrors the helper in hedge-recommendation.spec.ts.
 */
async function goToBookRiskTab(page: Page) {
  await page.route('**/api/v1/divisions', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([{ id: 'div-1', name: 'Equities', description: '', deskCount: 1 }]),
    })
  })
  await page.route('**/api/v1/divisions/div-1/desks', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([{ id: 'desk-1', name: 'EU Equities', divisionId: 'div-1', deskHead: 'Alice', bookCount: 1 }]),
    })
  })

  await page.goto('/')

  await page.getByTestId('hierarchy-selector-toggle').click()
  await page.waitForSelector('[data-testid="hierarchy-division-div-1"]')
  await page.getByTestId('hierarchy-division-div-1').click()
  await page.waitForSelector('[data-testid="hierarchy-desk-desk-1"]')
  await page.getByTestId('hierarchy-desk-desk-1').click()
  await page.waitForSelector('[data-testid="hierarchy-book-port-1"]')
  await page.getByTestId('hierarchy-book-port-1').click()

  await page.getByTestId('tab-risk').click()
}

test.describe('Canonical LiquidityTier labels in UI [spec-drift A-9]', () => {
  test.beforeEach(async ({ page }) => {
    await mockAllApiRoutes(page)
  })

  test('hedge suggestion shows HIGH_LIQUID tier label and not TIER_1', async ({ page }) => {
    const rec = {
      ...TEST_HEDGE_RECOMMENDATION,
      suggestions: [
        { ...TEST_HEDGE_RECOMMENDATION.suggestions[0], liquidityTier: 'HIGH_LIQUID' },
      ],
    }
    await mockHedgeSuggest(page, rec)

    await goToBookRiskTab(page)
    await page.waitForSelector('[data-testid="suggest-hedge-open-button"]')
    await page.getByTestId('suggest-hedge-open-button').click()
    await page.getByTestId('suggest-hedge-button').click()

    await expect(page.getByTestId('suggestion-1')).toBeVisible()
    // Canonical label is rendered
    await expect(page.getByTestId('suggestion-1')).toContainText('HIGH_LIQUID')
    // Old non-canonical label must NOT appear
    await expect(page.getByTestId('suggestion-1')).not.toContainText('TIER_1')
  })

  test('hedge suggestion shows LIQUID tier label and not TIER_2', async ({ page }) => {
    const rec = {
      ...TEST_HEDGE_RECOMMENDATION,
      suggestions: [
        { ...TEST_HEDGE_RECOMMENDATION.suggestions[0], liquidityTier: 'LIQUID' },
      ],
    }
    await mockHedgeSuggest(page, rec)

    await goToBookRiskTab(page)
    await page.waitForSelector('[data-testid="suggest-hedge-open-button"]')
    await page.getByTestId('suggest-hedge-open-button').click()
    await page.getByTestId('suggest-hedge-button').click()

    await expect(page.getByTestId('suggestion-1')).toBeVisible()
    await expect(page.getByTestId('suggestion-1')).toContainText('LIQUID')
    await expect(page.getByTestId('suggestion-1')).not.toContainText('TIER_2')
  })

  test('hedge suggestion shows SEMI_LIQUID tier label and not TIER_3', async ({ page }) => {
    const rec = {
      ...TEST_HEDGE_RECOMMENDATION,
      suggestions: [
        { ...TEST_HEDGE_RECOMMENDATION.suggestions[0], liquidityTier: 'SEMI_LIQUID' },
      ],
    }
    await mockHedgeSuggest(page, rec)

    await goToBookRiskTab(page)
    await page.waitForSelector('[data-testid="suggest-hedge-open-button"]')
    await page.getByTestId('suggest-hedge-open-button').click()
    await page.getByTestId('suggest-hedge-button').click()

    await expect(page.getByTestId('suggestion-1')).toBeVisible()
    await expect(page.getByTestId('suggestion-1')).toContainText('SEMI_LIQUID')
    await expect(page.getByTestId('suggestion-1')).not.toContainText('TIER_3')
  })

  test('hedge suggestion shows ILLIQUID tier label', async ({ page }) => {
    const rec = {
      ...TEST_HEDGE_RECOMMENDATION,
      suggestions: [
        { ...TEST_HEDGE_RECOMMENDATION.suggestions[0], liquidityTier: 'ILLIQUID' },
      ],
    }
    await mockHedgeSuggest(page, rec)

    await goToBookRiskTab(page)
    await page.waitForSelector('[data-testid="suggest-hedge-open-button"]')
    await page.getByTestId('suggest-hedge-open-button').click()
    await page.getByTestId('suggest-hedge-button').click()

    await expect(page.getByTestId('suggestion-1')).toBeVisible()
    await expect(page.getByTestId('suggestion-1')).toContainText('ILLIQUID')
  })
})
