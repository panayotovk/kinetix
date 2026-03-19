import { test, expect } from '@playwright/test'
import { mockAllApiRoutes } from './fixtures'

// Injects a WebSocket that always fails immediately, simulating MAX_RECONNECT_ATTEMPTS (20)
// failures in rapid succession so the exhausted state is reached.
async function injectExhaustingWebSocket(page: import('@playwright/test').Page) {
  await page.addInitScript(() => {
    const OriginalWebSocket = window.WebSocket

    class ExhaustingWebSocket extends EventTarget {
      static CONNECTING = 0
      static OPEN = 1
      static CLOSING = 2
      static CLOSED = 3
      CONNECTING = 0
      OPEN = 1
      CLOSING = 2
      CLOSED = 3

      readyState = 0
      url: string
      protocol = ''
      extensions = ''
      bufferedAmount = 0
      binaryType: BinaryType = 'blob'
      onopen: ((this: WebSocket, ev: Event) => void) | null = null
      onclose: ((this: WebSocket, ev: CloseEvent) => void) | null = null
      onmessage: ((this: WebSocket, ev: MessageEvent) => void) | null = null
      onerror: ((this: WebSocket, ev: Event) => void) | null = null

      constructor(url: string | URL, _protocols?: string | string[]) {
        super()
        this.url = typeof url === 'string' ? url : url.toString()

        // Fail immediately with no delay so tests run fast
        setTimeout(() => {
          this.readyState = 3
          const errorEvent = new Event('error')
          if (this.onerror) this.onerror.call(this as unknown as WebSocket, errorEvent)
          this.dispatchEvent(errorEvent)
          const closeEvent = new CloseEvent('close', { code: 1006, reason: 'Connection refused' })
          if (this.onclose) this.onclose.call(this as unknown as WebSocket, closeEvent)
          this.dispatchEvent(closeEvent)
        }, 1)
      }

      send(_data: string | ArrayBuffer | Blob | ArrayBufferView): void {}

      close(_code?: number, _reason?: string): void {
        this.readyState = 3
      }

      addEventListener(type: string, listener: EventListenerOrEventListenerObject, options?: boolean | AddEventListenerOptions): void {
        super.addEventListener(type, listener, options)
      }

      removeEventListener(type: string, listener: EventListenerOrEventListenerObject, options?: boolean | EventListenerOptions): void {
        super.removeEventListener(type, listener, options)
      }
    }

    ;(window as unknown as Record<string, unknown>).WebSocket = function (url: string | URL, protocols?: string | string[]) {
      const urlStr = typeof url === 'string' ? url : url.toString()
      if (urlStr.includes('/ws/prices')) {
        return new ExhaustingWebSocket(url, protocols)
      }
      return new OriginalWebSocket(url, protocols)
    } as unknown as typeof WebSocket
    Object.defineProperty((window as unknown as Record<string, unknown>).WebSocket, 'CONNECTING', { value: 0 })
    Object.defineProperty((window as unknown as Record<string, unknown>).WebSocket, 'OPEN', { value: 1 })
    Object.defineProperty((window as unknown as Record<string, unknown>).WebSocket, 'CLOSING', { value: 2 })
    Object.defineProperty((window as unknown as Record<string, unknown>).WebSocket, 'CLOSED', { value: 3 })

    // Override setTimeout to instantly execute reconnect timers so we exhaust quickly
    const originalSetTimeout = window.setTimeout
    ;(window as unknown as Record<string, unknown>).setTimeout = function (fn: () => void, delay?: number, ...args: unknown[]) {
      // Collapse reconnect backoff delays to near-zero while preserving small real delays
      const effectiveDelay = (delay && delay > 10) ? 1 : delay
      return originalSetTimeout(fn, effectiveDelay, ...args)
    } as typeof setTimeout
  })
}

test.describe('WebSocket Exhaustion — Connection Lost Banner', () => {
  test.beforeEach(async ({ page }) => {
    await mockAllApiRoutes(page)
  })

  test('shows "Connection lost" banner with Reconnect button after exhaustion', async ({ page }) => {
    await injectExhaustingWebSocket(page)

    await page.goto('/')

    // Wait for the connection-lost banner to appear (after 20 failed attempts)
    const banner = page.getByTestId('connection-lost-banner')
    await expect(banner).toBeVisible({ timeout: 15000 })
    await expect(banner).toContainText('Connection lost')

    // Reconnect button must be present
    const reconnectBtn = page.getByTestId('reconnect-button')
    await expect(reconnectBtn).toBeVisible()
    await expect(reconnectBtn).toContainText('Reconnect')
  })

  test('connection-lost banner has role="alert" for screen reader accessibility', async ({ page }) => {
    await injectExhaustingWebSocket(page)

    await page.goto('/')

    const banner = page.getByTestId('connection-lost-banner')
    await expect(banner).toBeVisible({ timeout: 15000 })
    await expect(banner).toHaveAttribute('role', 'alert')
  })

  test('reconnecting banner is not shown when connection is exhausted', async ({ page }) => {
    await injectExhaustingWebSocket(page)

    await page.goto('/')

    // Wait for exhaustion state
    const lostBanner = page.getByTestId('connection-lost-banner')
    await expect(lostBanner).toBeVisible({ timeout: 15000 })

    // The reconnecting banner should not be visible in the exhausted state
    await expect(page.getByTestId('reconnecting-banner')).not.toBeVisible()
  })
})
