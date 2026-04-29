import { test, expect, Page } from '@playwright/test'
import { mockAllApiRoutes, mockLimitsRoutes } from './fixtures'

async function goToRiskTab(page: Page) {
  await page.goto('/')
  await page.getByTestId('tab-risk').click()
}

test.describe('Limits Panel', () => {
  test.beforeEach(async ({ page }) => {
    await mockAllApiRoutes(page)
  })

  test('renders limits grouped by hierarchy level (FIRM through TRADER)', async ({ page }) => {
    await mockLimitsRoutes(page, {
      limits: [
        {
          id: 'l-firm',
          level: 'FIRM',
          entityId: 'firm-1',
          limitType: 'VAR',
          limitValue: '1000000',
          intradayLimit: null,
          overnightLimit: null,
          active: true,
        },
        {
          id: 'l-desk',
          level: 'DESK',
          entityId: 'desk-eq',
          limitType: 'NOTIONAL',
          limitValue: '5000000',
          intradayLimit: '4500000',
          overnightLimit: null,
          active: true,
        },
        {
          id: 'l-trader',
          level: 'TRADER',
          entityId: 'trader-a',
          limitType: 'POSITION',
          limitValue: '10000',
          intradayLimit: null,
          overnightLimit: null,
          active: true,
        },
      ],
    })

    await goToRiskTab(page)
    await page.waitForSelector('[data-testid="limits-panel"]')

    await expect(page.getByTestId('limits-group-FIRM')).toBeVisible()
    await expect(page.getByTestId('limits-group-DESK')).toBeVisible()
    await expect(page.getByTestId('limits-group-TRADER')).toBeVisible()

    await expect(page.getByTestId('limits-row-l-firm')).toContainText('firm-1')
    await expect(page.getByTestId('limits-row-l-firm')).toContainText('VAR')
    await expect(page.getByTestId('limits-row-l-firm')).toContainText('1,000,000')
    await expect(page.getByTestId('limits-row-l-desk')).toContainText('4,500,000')
  })

  test('inactive limits render with the inactive indicator instead of the active dot', async ({ page }) => {
    await mockLimitsRoutes(page, {
      limits: [
        {
          id: 'l-active',
          level: 'FIRM',
          entityId: 'firm-active',
          limitType: 'VAR',
          limitValue: '1000000',
          intradayLimit: null,
          overnightLimit: null,
          active: true,
        },
        {
          id: 'l-disabled',
          level: 'FIRM',
          entityId: 'firm-disabled',
          limitType: 'VAR',
          limitValue: '500000',
          intradayLimit: null,
          overnightLimit: null,
          active: false,
        },
      ],
    })

    await goToRiskTab(page)
    await page.waitForSelector('[data-testid="limits-panel"]')

    const activeRow = page.getByTestId('limits-row-l-active')
    const disabledRow = page.getByTestId('limits-row-l-disabled')
    await expect(activeRow).toContainText('●')
    await expect(disabledRow).toContainText('○')
  })

  test('the level filter narrows the visible groups to the chosen hierarchy level', async ({ page }) => {
    await mockLimitsRoutes(page, {
      limits: [
        {
          id: 'l-firm',
          level: 'FIRM',
          entityId: 'firm-1',
          limitType: 'VAR',
          limitValue: '1000000',
          intradayLimit: null,
          overnightLimit: null,
          active: true,
        },
        {
          id: 'l-desk',
          level: 'DESK',
          entityId: 'desk-eq',
          limitType: 'NOTIONAL',
          limitValue: '5000000',
          intradayLimit: null,
          overnightLimit: null,
          active: true,
        },
      ],
    })

    await goToRiskTab(page)
    await page.waitForSelector('[data-testid="limits-panel"]')

    await page.getByTestId('limits-level-filter').selectOption('DESK')

    await expect(page.getByTestId('limits-group-DESK')).toBeVisible()
    await expect(page.getByTestId('limits-group-FIRM')).toBeHidden()
  })

  test('shows the empty state when no limits are configured', async ({ page }) => {
    // Default fixture stub returns [] — no override needed.
    await goToRiskTab(page)
    await page.waitForSelector('text=No limits defined')

    await expect(page.getByText('No limits defined')).toBeVisible()
  })
})
