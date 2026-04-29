# Kinetix UI

React + TypeScript trading and risk dashboard. Talks to the Kinetix gateway over REST and to the notification-service over WebSocket for live updates.

## Tech Stack

- **React 19** + **TypeScript 5.9** + **Vite 7**
- **Tailwind CSS 4** for styling, with class-based dark mode
- **Recharts** for risk and P&L charting
- **Vitest** for unit tests, **Playwright** for browser E2E tests
- **ESLint** with React-hooks rules — `react-hooks/set-state-in-effect` is enforced

## Tabs

The dashboard exposes 11 trader/risk tabs:

| Tab | Purpose |
|---|---|
| **Positions** | Live position grid with column toggles, pagination, CSV export |
| **Trades** | Trade booking + sub-tabs for blotter, execution-cost analysis, prime-broker reconciliation |
| **P&L** | Intraday and daily P&L with Greek attribution |
| **Risk** | VaR, ES, Greeks, factor decomposition, hedge recommendations |
| **EoD Timeline** | End-of-day risk and P&L history |
| **Scenarios** | Stress testing and reverse stress testing |
| **Regulatory** | FRTB capital, backtests, submissions |
| **Counterparty Risk** | PFE/EPE/CVA aggregation across netting sets |
| **Reports** | On-demand and scheduled reports |
| **Alerts** | Limit breaches, anomalies, suggested actions |
| **System** | Service health, market regime, data quality |

## Development

```bash
npm install
npm run dev          # Vite dev server on http://localhost:5173
npm run dev:demo     # Demo mode with mocked data on port 5174
```

The dev server proxies API calls to the gateway at `http://localhost:8080`. Make sure the backend is running (`./dev-up.sh` from the repo root) or use demo mode.

## Testing

```bash
npm run test                   # Vitest unit tests
npm run playwright             # Playwright E2E (headless)
npm run playwright:headed      # Playwright with browser visible
npm run playwright:ui          # Playwright UI mode (interactive)
npm run playwright:demo        # Playwright against demo mode (no backend needed)
```

Run `npm run lint` before committing — ESLint catches React-hooks bugs that unit tests miss. See `CLAUDE.md` for testing expectations.

## Build

```bash
npm run build        # Type-check + production bundle to dist/
npm run preview      # Preview the production build locally
```

## Layout

```
ui/src/
├── api/             HTTP client wrappers per backend service
├── auth/            Keycloak / OAuth2 integration
├── components/      One folder/file per tab and panel
├── constants/       Shared enums and config
├── hooks/           Custom hooks (useTheme, useWorkspace, useMarketRegime, ...)
├── test-utils/      Vitest helpers and mocks
├── types.ts         Shared TypeScript types
├── utils/           Pure helpers (formatting, exportToCsv, ...)
├── App.tsx          Top-level layout, tab routing, keyboard navigation
└── main.tsx         React entry point
```

## Conventions

- **Accessibility**: tabs follow the WAI-ARIA pattern with full keyboard navigation (arrow keys, Home/End). New interactive components must include appropriate `aria-*` attributes.
- **Persistence**: user preferences (theme, column visibility, default tab, alert rules) live in `localStorage`, accessed through dedicated hooks.
- **Real-time updates**: WebSocket streams (`usePriceStream`, `useAlertStream`, ...) auto-reconnect with exponential backoff, max 20 attempts, and surface a "reconnecting" banner during disconnection.
- **Tests for new features**: every meaningful UI feature needs both Vitest unit tests *and* Playwright E2E coverage. See `e2e/fixtures.ts` for the API-mocking pattern.
