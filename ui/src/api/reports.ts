export interface ReportTemplate {
  templateId: string
  name: string
  templateType: string
  ownerUserId: string
  description: string
  source: string
}

export interface ReportOutput {
  outputId: string
  templateId: string
  generatedAt: string
  outputFormat: string
  rowCount: number
}

export interface GenerateReportRequest {
  templateId: string
  bookId: string
  date?: string
  format?: string
}

export async function fetchReportTemplates(): Promise<ReportTemplate[]> {
  const response = await fetch('/api/v1/reports/templates')
  if (!response.ok) {
    throw new Error(
      `Failed to fetch report templates: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}

export async function generateReport(request: GenerateReportRequest): Promise<ReportOutput> {
  const response = await fetch('/api/v1/reports/generate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) {
    throw new Error(
      `Failed to generate report: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}

export async function fetchReportOutput(outputId: string): Promise<ReportOutput | null> {
  const response = await fetch(`/api/v1/reports/${encodeURIComponent(outputId)}`)
  if (response.status === 404) {
    return null
  }
  if (!response.ok) {
    throw new Error(
      `Failed to fetch report output: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}

export async function downloadReportCsv(outputId: string): Promise<string | null> {
  const response = await fetch(`/api/v1/reports/${encodeURIComponent(outputId)}/csv`)
  if (response.status === 404) {
    return null
  }
  if (!response.ok) {
    throw new Error(
      `Failed to download report CSV: ${response.status} ${response.statusText}`,
    )
  }
  return response.text()
}
