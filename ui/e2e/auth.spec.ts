import { test, expect } from '@playwright/test'
import { mockAllApiRoutes, mockKeycloakAuth } from './fixtures'

test.describe('Authentication', () => {
  test.beforeEach(async ({ page }) => {
    await mockAllApiRoutes(page)
  })

  test('shows username in header when authenticated', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByTestId('header-username')).toHaveText('testuser')
  })

  test('shows role badge for authenticated user', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByTestId('header-role-badge')).toBeVisible()
    await expect(page.getByTestId('header-role-badge')).toHaveText('ADMIN')
  })

  test('logout button is visible and clickable', async ({ page }) => {
    await page.goto('/')
    const logoutButton = page.getByTestId('logout-button')
    await expect(logoutButton).toBeVisible()
    await expect(logoutButton).toHaveAttribute('aria-label', 'Log out')
  })

  test('app loads positions normally after authentication', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('[data-testid="position-row-AAPL"]')
    await expect(page.getByTestId('position-row-AAPL')).toBeVisible()
  })
})

test.describe('Role badge displays correctly for different roles', () => {
  test('TRADER role shows emerald badge', async ({ page }) => {
    await mockAllApiRoutes(page)
    await mockKeycloakAuth(page, { username: 'trader1', roles: ['TRADER'] })
    await page.goto('/')
    await expect(page.getByTestId('header-role-badge')).toHaveText('TRADER')
    await expect(page.getByTestId('header-username')).toHaveText('trader1')
  })

  test('RISK_MANAGER role shows blue badge', async ({ page }) => {
    await mockAllApiRoutes(page)
    await mockKeycloakAuth(page, { username: 'risk_mgr', roles: ['RISK_MANAGER'] })
    await page.goto('/')
    await expect(page.getByTestId('header-role-badge')).toHaveText('RISK MANAGER')
    await expect(page.getByTestId('header-username')).toHaveText('risk_mgr')
  })

  test('COMPLIANCE role shows amber badge', async ({ page }) => {
    await mockAllApiRoutes(page)
    await mockKeycloakAuth(page, { username: 'compliance1', roles: ['COMPLIANCE'] })
    await page.goto('/')
    await expect(page.getByTestId('header-role-badge')).toHaveText('COMPLIANCE')
  })
})
