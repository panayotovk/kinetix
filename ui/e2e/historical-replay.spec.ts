import { test, expect } from '@playwright/test'
import type { Page, Route } from '@playwright/test'
import { mockAllApiRoutes } from './fixtures'

// ---------------------------------------------------------------------------
// Fixture data
// ---------------------------------------------------------------------------

const HISTORICAL_SCENARIOS = [
  {
    id: 'sc-gfc',
    name: 'GFC_2008',
    description: 'Global Financial Crisis 2008',
    shocks: '{"equity":-0.10}',
    status: 'APPROVED',
    createdBy: 'system',
    approvedBy: 'system',
    approvedAt: '2024-01-01T00:00:00Z',
    createdAt: '2024-01-01T00:00:00Z',
    scenarioType: 'HISTORICAL_REPLAY',
  },
  {
    id: 'sc-covid',
    name: 'COVID_2020',
    description: 'COVID-19 market shock',
    shocks: '{"equity":-0.14}',
    status: 'APPROVED',
    createdBy: 'system',
    approvedBy: 'system',
    approvedAt: '2024-01-01T00:00:00Z',
    createdAt: '2024-01-01T00:00:00Z',
    scenarioType: 'HISTORICAL_REPLAY',
  },
  {
    id: 'sc-parametric',
    name: 'RATES_SHOCK',
    description: 'Parametric rates shock',
    shocks: '{"volShocks":{"FIXED_INCOME":1.5}}',
    status: 'APPROVED',
    createdBy: 'analyst',
    approvedBy: 'manager',
    approvedAt: '2024-01-01T00:00:00Z',
    createdAt: '2024-01-01T00:00:00Z',
    scenarioType: 'PARAMETRIC',
  },
]

const REPLAY_RESULT = {
  scenarioName: 'GFC_2008',
  totalPnlImpact: '-125000.00',
  positionImpacts: [
    {
      instrumentId: 'AAPL',
      assetClass: 'EQUITY',
      marketValue: '15500.00',
      pnlImpact: '-77500.00',
      dailyPnl: ['-31000.00', '-15500.00', '-31000.00'],
      proxyUsed: false,
    },
    {
      instrumentId: 'GOOGL',
      assetClass: 'EQUITY',
      marketValue: '142500.00',
      pnlImpact: '-47500.00',
      dailyPnl: ['-19000.00', '-9500.00', '-19000.00'],
      proxyUsed: true,
    },
  ],
  windowStart: '2008-09-15',
  windowEnd: '2008-09-19',
  calculatedAt: '2025-01-15T10:00:00Z',
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

async function goToScenariosTab(page: Page) {
  await page.goto('/')
  await page.getByTestId('tab-scenarios').click()
}

async function mockScenariosWithHistoricalRoutes(
  page: Page,
  opts: {
    governanceScenarios?: object[]
    replayResult?: object | null
    reverseStressResult?: object | null
  } = {},
): Promise<void> {
  await page.unroute('**/api/v1/risk/stress/scenarios')
  await page.unroute('**/api/v1/risk/**')

  // Catch-all for risk (lowest priority)
  await page.route('**/api/v1/risk/**', (route: Route) => {
    route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify(null) })
  })

  // Batch stress returns empty by default
  await page.route('**/api/v1/risk/stress/*/batch', (route: Route) => {
    if (route.request().method() === 'POST') {
      route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) })
    } else {
      route.fallback()
    }
  })

  // Historical replay endpoint
  await page.route('**/api/v1/risk/stress/*/historical-replay', (route: Route) => {
    if (route.request().method() === 'POST') {
      if (opts.replayResult) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(opts.replayResult),
        })
      } else {
        route.fulfill({ status: 500, body: 'Internal Server Error' })
      }
    } else {
      route.fallback()
    }
  })

  // Reverse stress endpoint
  await page.route('**/api/v1/risk/stress/*/reverse', (route: Route) => {
    if (route.request().method() === 'POST') {
      if (opts.reverseStressResult) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(opts.reverseStressResult),
        })
      } else {
        route.fulfill({ status: 500, body: 'Internal Server Error' })
      }
    } else {
      route.fallback()
    }
  })

  // Stress scenario names list
  await page.route('**/api/v1/risk/stress/scenarios', (route: Route) => {
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) })
  })

  // Governance CRUD
  const scenarios = [...(opts.governanceScenarios ?? [])] as Array<Record<string, unknown>>
  await page.route('**/api/v1/stress-scenarios/approved', (route: Route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(scenarios.filter((s) => s.status === 'APPROVED')),
    })
  })
  await page.route('**/api/v1/stress-scenarios/*/submit', (route: Route) => {
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({}) })
  })
  await page.route('**/api/v1/stress-scenarios/*/approve', (route: Route) => {
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({}) })
  })
  await page.route('**/api/v1/stress-scenarios/*/retire', (route: Route) => {
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({}) })
  })
  await page.route('**/api/v1/stress-scenarios', (route: Route) => {
    if (route.request().method() === 'POST') {
      route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({ id: 'sc-new', name: 'new', status: 'DRAFT', createdBy: 'user' }),
      })
    } else {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(scenarios),
      })
    }
  })
}

// ---------------------------------------------------------------------------
// Historical Replay Panel
// ---------------------------------------------------------------------------

test.describe('Historical Replay Panel', () => {
  test.beforeEach(async ({ page }) => {
    await mockAllApiRoutes(page)
  })

  test('historical replay panel is visible when APPROVED HISTORICAL_REPLAY scenarios exist', async ({
    page,
  }) => {
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: HISTORICAL_SCENARIOS,
      replayResult: REPLAY_RESULT,
    })

    await goToScenariosTab(page)
    await page.waitForSelector('[data-testid="historical-replay-panel"]')

    await expect(page.getByTestId('historical-replay-panel')).toBeVisible()
  })

  test('historical replay panel is hidden when no HISTORICAL_REPLAY scenarios exist', async ({
    page,
  }) => {
    // Only parametric scenario
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: [HISTORICAL_SCENARIOS[2]], // RATES_SHOCK is PARAMETRIC
    })

    await goToScenariosTab(page)
    await page.waitForSelector('[data-testid="scenarios-tab"]')

    await expect(page.getByTestId('historical-replay-panel')).not.toBeVisible()
  })

  test('scenario dropdown contains only HISTORICAL_REPLAY scenario names', async ({ page }) => {
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: HISTORICAL_SCENARIOS,
      replayResult: REPLAY_RESULT,
    })

    await goToScenariosTab(page)
    await page.waitForSelector('[data-testid="replay-scenario-dropdown"]')

    const dropdown = page.getByTestId('replay-scenario-dropdown')
    await expect(dropdown).toBeVisible()

    const options = await dropdown.locator('option').allTextContents()
    expect(options.some((o) => o.includes('GFC'))).toBeTruthy()
    expect(options.some((o) => o.includes('COVID'))).toBeTruthy()
    expect(options.some((o) => o.includes('RATES_SHOCK'))).toBeFalsy()
  })

  test('clicking Run Replay fetches result and displays P&L impact', async ({ page }) => {
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: HISTORICAL_SCENARIOS,
      replayResult: REPLAY_RESULT,
    })

    await goToScenariosTab(page)
    await page.waitForSelector('[data-testid="replay-run-btn"]')

    await page.getByTestId('replay-run-btn').click()
    await page.waitForSelector('[data-testid="replay-results"]')

    await expect(page.getByTestId('replay-total-pnl')).toBeVisible()
    await expect(page.getByTestId('replay-position-impacts')).toBeVisible()
    // Scope to results panel to avoid strict mode violations
    const results = page.getByTestId('replay-position-impacts')
    await expect(results.getByText('AAPL')).toBeVisible()
    await expect(results.getByText('GOOGL')).toBeVisible()
  })

  test('shows proxy badge for positions that used proxy returns', async ({ page }) => {
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: HISTORICAL_SCENARIOS,
      replayResult: REPLAY_RESULT,
    })

    await goToScenariosTab(page)
    await page.waitForSelector('[data-testid="replay-run-btn"]')

    await page.getByTestId('replay-run-btn').click()
    await page.waitForSelector('[data-testid="proxy-badge-GOOGL"]')

    await expect(page.getByTestId('proxy-badge-GOOGL')).toBeVisible()
    await expect(page.getByTestId('proxy-badge-GOOGL')).toContainText('proxy')
    await expect(page.getByTestId('proxy-badge-AAPL')).not.toBeVisible()
  })

  test('shows historical window dates in result', async ({ page }) => {
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: HISTORICAL_SCENARIOS,
      replayResult: REPLAY_RESULT,
    })

    await goToScenariosTab(page)
    await page.getByTestId('replay-run-btn').click()
    await page.waitForSelector('[data-testid="replay-results"]')

    await expect(page.getByText(/2008-09-15/)).toBeVisible()
    await expect(page.getByText(/2008-09-19/)).toBeVisible()
  })
})

// ---------------------------------------------------------------------------
// Reverse Stress Dialog
// ---------------------------------------------------------------------------

test.describe('Reverse Stress Dialog', () => {
  test.beforeEach(async ({ page }) => {
    await mockAllApiRoutes(page)
  })

  test('reverse stress dialog is not shown by default', async ({ page }) => {
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: HISTORICAL_SCENARIOS,
    })

    await goToScenariosTab(page)
    await page.waitForSelector('[data-testid="scenarios-tab"]')

    await expect(page.getByTestId('reverse-stress-dialog')).not.toBeVisible()
  })
})

// ---------------------------------------------------------------------------
// Scenario Library Grid
// ---------------------------------------------------------------------------

test.describe('Scenario Library Grid', () => {
  test.beforeEach(async ({ page }) => {
    await mockAllApiRoutes(page)
  })

  test('library grid appears when Manage Scenarios is clicked', async ({ page }) => {
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: HISTORICAL_SCENARIOS,
    })

    await goToScenariosTab(page)
    await page.waitForSelector('[data-testid="manage-scenarios-btn"]')

    await page.getByTestId('manage-scenarios-btn').click()
    await page.waitForSelector('[data-testid="scenario-library-grid"]')

    await expect(page.getByTestId('scenario-library-grid')).toBeVisible()
  })

  test('library grid shows all scenarios with type badges', async ({ page }) => {
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: HISTORICAL_SCENARIOS,
    })

    await goToScenariosTab(page)
    await page.getByTestId('manage-scenarios-btn').click()
    await page.waitForSelector('[data-testid="scenario-library-grid"]')

    // Scope to grid to avoid strict mode violation (governance-panel also lists names)
    const grid = page.getByTestId('scenario-library-grid')
    await expect(grid.getByText('GFC_2008')).toBeVisible()
    await expect(grid.getByText('COVID_2020')).toBeVisible()
    await expect(grid.getByText('RATES_SHOCK')).toBeVisible()

    const typeBadges = grid.getByTestId('scenario-type-badge')
    await expect(typeBadges).toHaveCount(3)
  })

  test('library grid search filters scenarios by name', async ({ page }) => {
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: HISTORICAL_SCENARIOS,
    })

    await goToScenariosTab(page)
    await page.getByTestId('manage-scenarios-btn').click()
    await page.waitForSelector('[data-testid="scenario-library-search"]')

    await page.getByTestId('scenario-library-search').fill('GFC')

    const grid = page.getByTestId('scenario-library-grid')
    await expect(grid.getByText('GFC_2008')).toBeVisible()
    // After filter, only one row should match
    await expect(grid.getByTestId('scenario-library-row')).toHaveCount(1)
  })

  test('library grid type filter shows only HISTORICAL_REPLAY scenarios', async ({ page }) => {
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: HISTORICAL_SCENARIOS,
    })

    await goToScenariosTab(page)
    await page.getByTestId('manage-scenarios-btn').click()
    await page.waitForSelector('[data-testid="scenario-library-type-filter"]')

    await page.getByTestId('scenario-library-type-filter').selectOption('HISTORICAL_REPLAY')

    const grid = page.getByTestId('scenario-library-grid')
    await expect(grid.getByTestId('scenario-library-row')).toHaveCount(2)
    await expect(grid.getByText('GFC_2008')).toBeVisible()
    await expect(grid.getByText('COVID_2020')).toBeVisible()
  })

  test('library grid type filter shows only PARAMETRIC scenarios', async ({ page }) => {
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: HISTORICAL_SCENARIOS,
    })

    await goToScenariosTab(page)
    await page.getByTestId('manage-scenarios-btn').click()
    await page.waitForSelector('[data-testid="scenario-library-type-filter"]')

    await page.getByTestId('scenario-library-type-filter').selectOption('PARAMETRIC')

    const grid = page.getByTestId('scenario-library-grid')
    await expect(grid.getByTestId('scenario-library-row')).toHaveCount(1)
    await expect(grid.getByText('RATES_SHOCK')).toBeVisible()
  })

  test('library grid shows empty state when search matches nothing', async ({ page }) => {
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: HISTORICAL_SCENARIOS,
    })

    await goToScenariosTab(page)
    await page.getByTestId('manage-scenarios-btn').click()
    await page.waitForSelector('[data-testid="scenario-library-search"]')

    await page.getByTestId('scenario-library-search').fill('NONEXISTENT_SCENARIO_XYZ_12345')

    await expect(page.getByTestId('scenario-library-empty')).toBeVisible()
  })

  test('library grid sort by name toggles to descending order', async ({ page }) => {
    await mockScenariosWithHistoricalRoutes(page, {
      governanceScenarios: HISTORICAL_SCENARIOS,
    })

    await goToScenariosTab(page)
    await page.getByTestId('manage-scenarios-btn').click()
    await page.waitForSelector('[data-testid="sort-by-name"]')

    // Default is ascending. One click toggles to descending.
    await page.getByTestId('sort-by-name').click()

    const rows = page.getByTestId('scenario-library-row')
    const firstRowText = await rows.first().textContent()
    // Descending: RATES_SHOCK > GFC_2008 > COVID_2020
    expect(firstRowText).toContain('RATES_SHOCK')
  })
})
