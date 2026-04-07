import { type SelectHTMLAttributes } from 'react'

type SelectProps = SelectHTMLAttributes<HTMLSelectElement>

export function Select({ className = '', ...rest }: SelectProps) {
  return (
    <select
      className={`border border-slate-300 dark:border-surface-600 rounded-md px-3 py-1.5 text-sm bg-white dark:bg-surface-700 dark:text-slate-200 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 transition-colors ${className}`}
      {...rest}
    />
  )
}
