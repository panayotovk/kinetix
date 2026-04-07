interface SubTab<T extends string> {
  key: T
  label: string
  'data-testid'?: string
}

interface SubTabsProps<T extends string> {
  tabs: SubTab<T>[]
  activeTab: T
  onTabChange: (tab: T) => void
  'aria-label'?: string
  testIdPrefix?: string
}

export function SubTabs<T extends string>({
  tabs,
  activeTab,
  onTabChange,
  'aria-label': ariaLabel,
  testIdPrefix,
}: SubTabsProps<T>) {
  return (
    <div className="flex gap-1 mb-4 border-b border-slate-200 dark:border-surface-700" role="tablist" aria-label={ariaLabel}>
      {tabs.map((tab) => (
        <button
          key={tab.key}
          role="tab"
          aria-selected={activeTab === tab.key}
          data-testid={tab['data-testid'] ?? (testIdPrefix ? `${testIdPrefix}-${tab.key}` : undefined)}
          onClick={() => onTabChange(tab.key)}
          className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeTab === tab.key
              ? 'border-primary-500 text-primary-600 dark:text-primary-400'
              : 'border-transparent text-slate-500 dark:text-slate-400 hover:text-slate-700 dark:hover:text-slate-300'
          }`}
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}
