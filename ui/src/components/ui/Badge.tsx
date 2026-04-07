import { type ReactNode } from 'react'

const variants = {
  critical: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300',
  warning: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300',
  info: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300',
  success: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300',
  neutral: 'bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-300',
  sod: 'bg-sky-100 text-sky-800 dark:bg-sky-900/30 dark:text-sky-300',
  eod: 'bg-amber-100 text-amber-800 ring-1 ring-amber-300 dark:bg-amber-900/30 dark:text-amber-300 dark:ring-amber-700',
  preclose: 'bg-purple-100 text-purple-800 ring-1 ring-purple-300 dark:bg-purple-900/30 dark:text-purple-300 dark:ring-purple-700',
} as const

interface BadgeProps {
  variant?: keyof typeof variants
  children: ReactNode
  'data-testid'?: string
}

export function Badge({ variant = 'neutral', children, ...rest }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${variants[variant]}`}
      {...rest}
    >
      {children}
    </span>
  )
}
