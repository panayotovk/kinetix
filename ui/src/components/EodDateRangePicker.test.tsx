import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { EodDateRangePicker } from './EodDateRangePicker'

function todayStr() {
  const today = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`
}

describe('EodDateRangePicker', () => {
  it('renders all preset buttons', () => {
    const onRangeChange = vi.fn()
    render(
      <EodDateRangePicker from="2026-02-01" to="2026-03-15" onRangeChange={onRangeChange} />,
    )

    expect(screen.getByTestId('eod-preset-1W')).toBeInTheDocument()
    expect(screen.getByTestId('eod-preset-1M')).toBeInTheDocument()
    expect(screen.getByTestId('eod-preset-3M')).toBeInTheDocument()
    expect(screen.getByTestId('eod-preset-YTD')).toBeInTheDocument()
    expect(screen.getByTestId('eod-preset-Custom')).toBeInTheDocument()
  })

  it('fires onRangeChange when 1W preset is clicked', () => {
    const onRangeChange = vi.fn()
    render(
      <EodDateRangePicker from="2026-02-01" to="2026-03-15" onRangeChange={onRangeChange} />,
    )

    fireEvent.click(screen.getByTestId('eod-preset-1W'))

    expect(onRangeChange).toHaveBeenCalledOnce()
    const [from, to] = onRangeChange.mock.calls[0]
    // from should be 7 days before today
    expect(to).toBe(todayStr())
    const fromDate = new Date(from)
    const toDate = new Date(to)
    const diffDays = Math.round((toDate.getTime() - fromDate.getTime()) / (24 * 60 * 60 * 1000))
    expect(diffDays).toBe(7)
  })

  it('fires onRangeChange when YTD preset is clicked', () => {
    const onRangeChange = vi.fn()
    render(
      <EodDateRangePicker from="2026-02-01" to="2026-03-15" onRangeChange={onRangeChange} />,
    )

    fireEvent.click(screen.getByTestId('eod-preset-YTD'))

    expect(onRangeChange).toHaveBeenCalledOnce()
    const [from] = onRangeChange.mock.calls[0]
    expect(from).toMatch(/^\d{4}-01-01$/)
  })

  it('renders custom date inputs when Custom is clicked', () => {
    const onRangeChange = vi.fn()
    render(
      <EodDateRangePicker from="2026-02-01" to="2026-03-15" onRangeChange={onRangeChange} />,
    )

    fireEvent.click(screen.getByTestId('eod-preset-Custom'))

    expect(screen.getByTestId('eod-custom-range-inputs')).toBeInTheDocument()
    expect(screen.getByTestId('eod-custom-from')).toBeInTheDocument()
    expect(screen.getByTestId('eod-custom-to')).toBeInTheDocument()
    expect(screen.getByTestId('eod-custom-apply')).toBeInTheDocument()
  })

  it('fires onRangeChange with custom dates when Apply is clicked', () => {
    const onRangeChange = vi.fn()
    render(
      <EodDateRangePicker from="2026-02-01" to="2026-03-15" onRangeChange={onRangeChange} />,
    )

    fireEvent.click(screen.getByTestId('eod-preset-Custom'))

    fireEvent.change(screen.getByTestId('eod-custom-from'), {
      target: { value: '2025-01-01' },
    })
    fireEvent.change(screen.getByTestId('eod-custom-to'), {
      target: { value: '2025-03-31' },
    })
    fireEvent.click(screen.getByTestId('eod-custom-apply'))

    expect(onRangeChange).toHaveBeenCalledWith('2025-01-01', '2025-03-31')
  })

  it('hides custom inputs when a different preset is selected after Custom', () => {
    const onRangeChange = vi.fn()
    render(
      <EodDateRangePicker from="2026-02-01" to="2026-03-15" onRangeChange={onRangeChange} />,
    )

    fireEvent.click(screen.getByTestId('eod-preset-Custom'))
    expect(screen.getByTestId('eod-custom-range-inputs')).toBeInTheDocument()

    fireEvent.click(screen.getByTestId('eod-preset-1M'))
    expect(screen.queryByTestId('eod-custom-range-inputs')).not.toBeInTheDocument()
  })
})
