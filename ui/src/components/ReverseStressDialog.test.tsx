import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ReverseStressDialog } from './ReverseStressDialog'
import type { ReverseStressResultDto } from '../types'

const mockResult: ReverseStressResultDto = {
  shocks: [
    { instrumentId: 'AAPL', shock: '-0.120000' },
    { instrumentId: 'GOOGL', shock: '-0.085000' },
  ],
  achievedLoss: '98750.00',
  targetLoss: '100000.00',
  converged: true,
  calculatedAt: '2025-01-15T10:00:00Z',
}

const mockNotConvergedResult: ReverseStressResultDto = {
  shocks: [{ instrumentId: 'AAPL', shock: '-1.000000' }],
  achievedLoss: '45000.00',
  targetLoss: '100000.00',
  converged: false,
  calculatedAt: '2025-01-15T10:00:00Z',
}

describe('ReverseStressDialog', () => {
  it('renders nothing when closed', () => {
    render(
      <ReverseStressDialog
        open={false}
        onClose={() => {}}
        onRun={() => {}}
        result={null}
        loading={false}
        error={null}
      />,
    )
    expect(screen.queryByTestId('reverse-stress-dialog')).toBeNull()
  })

  it('renders the dialog when open', () => {
    render(
      <ReverseStressDialog
        open={true}
        onClose={() => {}}
        onRun={() => {}}
        result={null}
        loading={false}
        error={null}
      />,
    )
    expect(screen.getByTestId('reverse-stress-dialog')).toBeDefined()
  })

  it('renders target loss and max shock inputs', () => {
    render(
      <ReverseStressDialog
        open={true}
        onClose={() => {}}
        onRun={() => {}}
        result={null}
        loading={false}
        error={null}
      />,
    )
    expect(screen.getByTestId('reverse-stress-target-loss')).toBeDefined()
    expect(screen.getByTestId('reverse-stress-max-shock')).toBeDefined()
  })

  it('calls onRun with targetLoss and maxShock when Run button clicked', async () => {
    const onRun = vi.fn()
    render(
      <ReverseStressDialog
        open={true}
        onClose={vi.fn()}
        onRun={onRun}
        result={null}
        loading={false}
        error={null}
      />,
    )
    const targetLossInput = screen.getByTestId('reverse-stress-target-loss')
    await userEvent.clear(targetLossInput)
    await userEvent.type(targetLossInput, '100000')

    await userEvent.click(screen.getByTestId('reverse-stress-run-btn'))
    expect(onRun).toHaveBeenCalledWith({ targetLoss: 100000, maxShock: -1.0 })
  })

  it('calls onRun with custom maxShock when provided', async () => {
    const onRun = vi.fn()
    render(
      <ReverseStressDialog
        open={true}
        onClose={vi.fn()}
        onRun={onRun}
        result={null}
        loading={false}
        error={null}
      />,
    )
    const targetLossInput = screen.getByTestId('reverse-stress-target-loss')
    await userEvent.clear(targetLossInput)
    await userEvent.type(targetLossInput, '50000')

    const maxShockInput = screen.getByTestId('reverse-stress-max-shock')
    await userEvent.clear(maxShockInput)
    await userEvent.type(maxShockInput, '-0.5')

    await userEvent.click(screen.getByTestId('reverse-stress-run-btn'))
    expect(onRun).toHaveBeenCalledWith({ targetLoss: 50000, maxShock: -0.5 })
  })

  it('shows loading state when loading', () => {
    render(
      <ReverseStressDialog
        open={true}
        onClose={() => {}}
        onRun={() => {}}
        result={null}
        loading={true}
        error={null}
      />,
    )
    expect(screen.getByTestId('reverse-stress-loading')).toBeDefined()
  })

  it('shows error message when error present', () => {
    render(
      <ReverseStressDialog
        open={true}
        onClose={() => {}}
        onRun={() => {}}
        result={null}
        loading={false}
        error="Reverse stress failed: 500"
      />,
    )
    expect(screen.getByTestId('reverse-stress-error')).toBeDefined()
    expect(screen.getByText('Reverse stress failed: 500')).toBeDefined()
  })

  it('displays converged result with achieved loss and shock table', () => {
    render(
      <ReverseStressDialog
        open={true}
        onClose={() => {}}
        onRun={() => {}}
        result={mockResult}
        loading={false}
        error={null}
      />,
    )
    expect(screen.getByTestId('reverse-stress-results')).toBeDefined()
    expect(screen.getByTestId('reverse-stress-converged')).toBeDefined()
    expect(screen.getByTestId('reverse-stress-achieved-loss')).toBeDefined()
    expect(screen.getByTestId('reverse-stress-shock-table')).toBeDefined()
    expect(screen.getByText('AAPL')).toBeDefined()
    expect(screen.getByText('GOOGL')).toBeDefined()
  })

  it('shows not-converged warning when result did not converge', () => {
    render(
      <ReverseStressDialog
        open={true}
        onClose={() => {}}
        onRun={() => {}}
        result={mockNotConvergedResult}
        loading={false}
        error={null}
      />,
    )
    expect(screen.getByTestId('reverse-stress-not-converged')).toBeDefined()
  })

  it('calls onClose when close button is clicked', async () => {
    const onClose = vi.fn()
    render(
      <ReverseStressDialog
        open={true}
        onClose={onClose}
        onRun={() => {}}
        result={null}
        loading={false}
        error={null}
      />,
    )
    await userEvent.click(screen.getByTestId('reverse-stress-close'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('calls onClose when backdrop is clicked', async () => {
    const onClose = vi.fn()
    render(
      <ReverseStressDialog
        open={true}
        onClose={onClose}
        onRun={() => {}}
        result={null}
        loading={false}
        error={null}
      />,
    )
    await userEvent.click(screen.getByTestId('reverse-stress-backdrop'))
    expect(onClose).toHaveBeenCalledOnce()
  })
})
