import { test, expect, Page } from '@playwright/test'
import { mockAllApiRoutes, mockMarginRoutes } from './fixtures'

async function goToRiskTab(page: Page) {
  await page.goto('/')
  await page.getByTestId('tab-risk').click()
}

test.describe('Margin Panel', () => {
  test.beforeEach(async ({ page }) => {
    await mockAllApiRoutes(page)
  })

  test('renders initial / variation / total margin from the gateway response', async ({ page }) => {
    await mockMarginRoutes(page, {
      estimate: {
        initialMargin: '12500.00',
        variationMargin: '350.00',
        totalMargin: '12850.00',
        currency: 'USD',
      },
    })

    await goToRiskTab(page)
    await page.waitForSelector('[data-testid="margin-panel"]')

    await expect(page.getByTestId('margin-initial')).toContainText('$12,500.00')
    await expect(page.getByTestId('margin-variation')).toContainText('$350.00')
    await expect(page.getByTestId('margin-total')).toContainText('$12,850.00')
  })

  test('shows the no-data empty state when the gateway returns 404', async ({ page }) => {
    // Default fixture stub already returns 404 — no override needed.
    await goToRiskTab(page)
    await page.waitForSelector('text=No margin data')

    await expect(page.getByText('No margin data')).toBeVisible()
    await expect(page.getByTestId('margin-panel')).toBeHidden()
  })

  test('shows the error banner with retry when the gateway returns 500', async ({ page }) => {
    await mockMarginRoutes(page, { status: 500 })

    await goToRiskTab(page)
    await page.waitForSelector('[data-testid="margin-error"]')

    await expect(page.getByTestId('margin-error')).toContainText('Failed to fetch margin estimate')
    await expect(page.getByTestId('margin-retry-btn')).toBeVisible()
  })
})
