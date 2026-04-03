/**
 * types/api.ts
 * Общие типы для ответов API (Spring Boot backend).
 * Все ответы бэкенда должны быть обёрнуты в эти дженерики.
 */

/** Стандартная обёртка успешного ответа */
export interface ApiResponse<T> {
  data: T
  message?: string
}

/** Пагинированный ответ (Spring Page) */
export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

/** Структура ошибки от бэкенда */
export interface ApiError {
  statusCode: number
  message: string
  details?: Record<string, string[]>
}

/** Параметры пагинации для запросов */
export interface PaginationParams {
  page?: number
  size?: number
  sort?: string
}
