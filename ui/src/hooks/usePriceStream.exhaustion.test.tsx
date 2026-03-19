import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { PositionDto } from '../types'
import { usePriceStream } from './usePriceStream'

const makePosition = (overrides: Partial<PositionDto> = {}): PositionDto => ({
  bookId: 'book-1',
  instrumentId: 'AAPL',
  assetClass: 'EQUITY',
  quantity: '100',
  averageCost: { amount: '150.00', currency: 'USD' },
  marketPrice: { amount: '155.00', currency: 'USD' },
  marketValue: { amount: '15500.00', currency: 'USD' },
  unrealizedPnl: { amount: '500.00', currency: 'USD' },
  ...overrides,
})

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

describe('usePriceStream — exhaustion and manualReconnect', () => {
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
    const positions = [makePosition()]
    const { result } = renderHook(() => usePriceStream(positions, 'ws://localhost/ws'))

    expect(result.current.exhausted).toBe(false)
  })

  it('sets exhausted to true after 20 failed reconnect attempts', () => {
    const positions = [makePosition()]
    const { result } = renderHook(() => usePriceStream(positions, 'ws://localhost/ws'))

    const ws1 = MockWebSocket.instances[0]
    act(() => { ws1.simulateOpen() })
    act(() => { ws1.simulateClose() })

    // Exhaust all 20 reconnection attempts
    for (let i = 0; i < 20; i++) {
      act(() => { vi.advanceTimersByTime(30000) })
      const ws = MockWebSocket.instances[MockWebSocket.instances.length - 1]
      act(() => { ws.simulateClose() })
    }

    // After 20 attempts the hook should be exhausted
    expect(result.current.exhausted).toBe(true)
    expect(result.current.reconnecting).toBe(false)
  })

  it('manualReconnect resets exhausted state and reconnects', () => {
    const positions = [makePosition()]
    const { result } = renderHook(() => usePriceStream(positions, 'ws://localhost/ws'))

    const ws1 = MockWebSocket.instances[0]
    act(() => { ws1.simulateOpen() })
    act(() => { ws1.simulateClose() })

    // Exhaust all 20 reconnection attempts
    for (let i = 0; i < 20; i++) {
      act(() => { vi.advanceTimersByTime(30000) })
      const ws = MockWebSocket.instances[MockWebSocket.instances.length - 1]
      act(() => { ws.simulateClose() })
    }

    expect(result.current.exhausted).toBe(true)
    const countBeforeManual = MockWebSocket.instances.length

    act(() => { result.current.manualReconnect() })

    // A new WebSocket should have been created
    expect(MockWebSocket.instances.length).toBe(countBeforeManual + 1)
    expect(result.current.exhausted).toBe(false)
  })

  it('reconnecting is false when exhausted', () => {
    const positions = [makePosition()]
    const { result } = renderHook(() => usePriceStream(positions, 'ws://localhost/ws'))

    const ws1 = MockWebSocket.instances[0]
    act(() => { ws1.simulateOpen() })
    act(() => { ws1.simulateClose() })

    for (let i = 0; i < 20; i++) {
      act(() => { vi.advanceTimersByTime(30000) })
      const ws = MockWebSocket.instances[MockWebSocket.instances.length - 1]
      act(() => { ws.simulateClose() })
    }

    expect(result.current.exhausted).toBe(true)
    expect(result.current.reconnecting).toBe(false)
  })
})
