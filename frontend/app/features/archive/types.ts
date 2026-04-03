/**
 * features/archive/types.ts
 * Типы доменной модели «Репрессированный» (Person).
 *
 * Брендированные типы (PersonId) защищают от случайного смешивания ID сущностей.
 * Status использует строковый union для точного маппинга на бэкенд.
 */

/** Брендированный тип для ID персоны (защита от путаницы с другими ID) */
export type PersonId = number & { readonly __brand: 'PersonId' }

/** Статус верификации записи */
export type VerificationStatus = 'verified' | 'pending'

/** Основная модель репрессированного */
export interface Person {
  id: PersonId
  fullName: string
  birthYear: number
  deathYear: number | null
  region: string
  accusation: string       // Обвинение (статьи УК)
  status: VerificationStatus
  createdAt?: string
  updatedAt?: string
}

/** DTO для создания/обновления записи */
export interface PersonCreateDto {
  fullName: string
  birthYear: number
  deathYear?: number | null
  region: string
  accusation: string
}

/** DTO для формы добавления записи (строковые поля — до валидации) */
export interface PersonFormData {
  fullName: string
  birthYear: string
  deathYear: string
  region: string
  district: string
  profession: string
  accusation: string
  biography: string
}

/** Параметры фильтрации для списка */
export interface PersonFilters {
  search?: string
  region?: string
  status?: VerificationStatus
  accusationType?: string
  decade?: string
  yearFrom?: number
  yearTo?: number
}
