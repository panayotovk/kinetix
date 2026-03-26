import type { BookDto, PositionDto } from '../types'
import { authFetch } from '../auth/authFetch'

export async function fetchBooks(): Promise<BookDto[]> {
  const response = await authFetch('/api/v1/books')
  if (!response.ok) {
    throw new Error(
      `Failed to fetch books: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}

export async function fetchPositions(
  bookId: string,
): Promise<PositionDto[]> {
  const response = await authFetch(
    `/api/v1/books/${encodeURIComponent(bookId)}/positions`,
  )
  if (!response.ok) {
    throw new Error(
      `Failed to fetch positions: ${response.status} ${response.statusText}`,
    )
  }
  return response.json()
}
