import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAlertStream } from './useAlertStream'

vi.mock('../api/notifications', () => ({
  fetchAlerts: vi.fn().mockResolvedValue([]),
}))

class MockWebSocket {
  static instances: MockWebSocket[] = []

  url: string
  onopen: (() => void) | null = null
  onmessage: ((e: { data: string }) => void) | null = null
  onclose: (() => void) | null = null
  onerror: (() => void) | null = null
  readyState = 0
  sent: string[] = []
  closed = false

  constructor(url: string) {
    this.url = url
    MockWebSocket.instances.push(this)
  }

  send(data: string) {
    this.sent.push(data)
  }

  close() {
    this.closed = true
  }

  simulateOpen() {
    this.readyState = 1
    this.onopen?.()
  }

  simulateClose() {
    this.readyState = 3
    this.onclose?.()
  }
}

describe('useAlertStream — exhaustion and manualReconnect', () => {
  beforeEach(() => {
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  it('sets exhausted to false initially', () => {
    const { result } = renderHook(() => useAlertStream('ws://localhost/ws'))

    expect(result.current.exhausted).toBe(false)
  })

  it('sets exhausted to true after 20 failed reconnect attempts', async () => {
    const { result } = renderHook(() => useAlertStream('ws://localhost/ws'))

    const ws1 = MockWebSocket.instances[0]
    act(() => { ws1.simulateOpen() })
    await act(async () => { await vi.runAllTimersAsync() })
    act(() => { ws1.simulateClose() })

    for (let i = 0; i < 20; i++) {
      act(() => { vi.advanceTimersByTime(30000) })
      const ws = MockWebSocket.instances[MockWebSocket.instances.length - 1]
      act(() => { ws.simulateClose() })
    }

    expect(result.current.exhausted).toBe(true)
    expect(result.current.reconnecting).toBe(false)
  })

  it('manualReconnect resets exhausted state and reconnects', async () => {
    const { result } = renderHook(() => useAlertStream('ws://localhost/ws'))

    const ws1 = MockWebSocket.instances[0]
    act(() => { ws1.simulateOpen() })
    await act(async () => { await vi.runAllTimersAsync() })
    act(() => { ws1.simulateClose() })

    for (let i = 0; i < 20; i++) {
      act(() => { vi.advanceTimersByTime(30000) })
      const ws = MockWebSocket.instances[MockWebSocket.instances.length - 1]
      act(() => { ws.simulateClose() })
    }

    expect(result.current.exhausted).toBe(true)
    const countBefore = MockWebSocket.instances.length

    act(() => { result.current.manualReconnect() })

    expect(MockWebSocket.instances.length).toBe(countBefore + 1)
    expect(result.current.exhausted).toBe(false)
  })
})
