import { test, expect } from '@playwright/test'
import { mockKeycloakAuth, mockAllApiRoutes } from './fixtures'

test.describe('Book access denied (403)', () => {
  test('shows access denied message and hides Retry when positions return 403', async ({ page }) => {
    await mockKeycloakAuth(page, { roles: ['TRADER'], sub: 'trader-1' })

    // Mock books endpoint to return a book
    await page.route('**/api/v1/divisions', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    )
    await page.route('**/api/v1/books', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{ bookId: 'restricted-book' }]),
      }),
    )

    // Mock positions to return 403 (BOLA denial)
    await page.route('**/api/v1/books/*/positions', (route) =>
      route.fulfill({
        status: 403,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'forbidden', message: 'Access to book restricted-book is not permitted' }),
      }),
    )

    // Mock other API routes to prevent unhandled requests
    await page.route('**/api/v1/books/*/summary', (route) =>
      route.fulfill({ status: 403, contentType: 'application/json', body: '{}' }),
    )
    await page.route('**/api/v1/risk/**', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '{}' }),
    )
    await page.route('**/api/v1/price/**', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    )
    await page.route('**/api/v1/alerts/**', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    )
    await page.route('**/api/v1/alert-rules/**', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    )

    await page.goto('/')

    // Wait for the error card to appear
    const errorCard = page.getByTestId('load-error-card')
    await expect(errorCard).toBeVisible({ timeout: 10000 })

    // Should show "Access denied" heading, not "Failed to load positions"
    await expect(errorCard.getByText('Access denied')).toBeVisible()
    await expect(errorCard.getByText('do not have access')).toBeVisible()

    // Retry button should NOT be visible for 403 errors
    await expect(page.getByTestId('retry-load-button')).not.toBeVisible()
  })

  test('shows Retry button for non-403 errors', async ({ page }) => {
    await mockAllApiRoutes(page)

    // Override positions to return 500
    await page.unroute('**/api/v1/books/*/positions')
    await page.route('**/api/v1/books/*/positions', (route) =>
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'internal', message: 'Something went wrong' }),
      }),
    )

    await page.goto('/')

    const errorCard = page.getByTestId('load-error-card')
    await expect(errorCard).toBeVisible({ timeout: 10000 })

    // Should show generic error heading
    await expect(errorCard.getByText('Failed to load positions')).toBeVisible()

    // Retry button SHOULD be visible for non-403 errors
    await expect(page.getByTestId('retry-load-button')).toBeVisible()
  })
})
