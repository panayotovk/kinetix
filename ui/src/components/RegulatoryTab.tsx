import { useRegulatory } from '../hooks/useRegulatory'
import { RegulatoryDashboard } from './RegulatoryDashboard'

interface RegulatoryTabProps {
  bookId: string | null
}

export function RegulatoryTab({ bookId }: RegulatoryTabProps) {
  const regulatory = useRegulatory(bookId)

  return (
    <RegulatoryDashboard
      result={regulatory.result}
      loading={regulatory.loading}
      error={regulatory.error}
      onCalculate={regulatory.calculate}
      onDownloadCsv={regulatory.downloadCsv}
      onDownloadXbrl={regulatory.downloadXbrl}
    />
  )
}
