import { test, expect, type Page, type Route } from '@playwright/test'
import { TEST_POSITIONS, TEST_BOOKS } from './fixtures'

const POSITIONS_PATTERN = '**/api/v1/portfolios/*/positions'
const BOOKS_PATTERN = '**/api/v1/portfolios'

async function mockSuccessRoutes(page: Page) {
  await page.route('**/api/v1/health', (route: Route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        status: 'UP',
        services: {
          gateway: { status: 'UP' },
          'position-service': { status: 'UP' },
          'price-service': { status: 'UP' },
          'risk-orchestrator': { status: 'UP' },
          'notification-service': { status: 'UP' },
        },
      }),
    }),
  )

  await page.route('**/api/v1/portfolios/*/risk/var', (route: Route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(null),
    }),
  )

  await page.route('**/api/v1/data-quality', (route: Route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ overall: 'OK', checks: [] }),
    }),
  )

  await page.route('**/api/v1/hierarchy/**', (route: Route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
  )

  await page.route('**/api/v1/alerts**', (route: Route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
  )

  await page.route('**/api/v1/alert-rules**', (route: Route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
  )
}

test.describe('Initial load retry button', () => {
  test('shows error card with Retry button when initial load fails, then recovers on retry', async ({ page }) => {
    await mockSuccessRoutes(page)

    let booksCallCount = 0

    // First call to books returns 503, subsequent calls succeed
    await page.route(BOOKS_PATTERN, (route: Route) => {
      booksCallCount++
      if (booksCallCount === 1) {
        route.fulfill({
          status: 503,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Service Unavailable' }),
        })
      } else {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(TEST_BOOKS.map(b => ({ portfolioId: b.bookId }))),
        })
      }
    })

    await page.route(POSITIONS_PATTERN, (route: Route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(TEST_POSITIONS),
      }),
    )

    await page.goto('/')

    // Error card should appear
    const errorCard = page.getByTestId('load-error-card')
    await expect(errorCard).toBeVisible({ timeout: 5000 })

    // Retry button must be present
    const retryBtn = page.getByTestId('retry-load-button')
    await expect(retryBtn).toBeVisible()

    // Click retry — the second call succeeds
    await retryBtn.click()

    // The error card should disappear and positions load
    await expect(errorCard).not.toBeVisible({ timeout: 5000 })
    await expect(page.getByTestId('position-row-AAPL')).toBeVisible({ timeout: 5000 })
  })

  test('error card has role="alert" for screen reader accessibility', async ({ page }) => {
    await mockSuccessRoutes(page)

    await page.route(BOOKS_PATTERN, (route: Route) =>
      route.fulfill({
        status: 503,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Service Unavailable' }),
      }),
    )

    await page.goto('/')

    const errorCard = page.getByTestId('load-error-card')
    await expect(errorCard).toBeVisible({ timeout: 5000 })
    await expect(errorCard).toHaveAttribute('role', 'alert')
  })
})
