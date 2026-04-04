/**
 * features/archive/types.ts
 * Типы доменной модели «Репрессированный» (Person) и «Документ» (Document).
 */

/** Брендированный тип для ID персоны */
export type PersonId = number & { readonly __brand: 'PersonId' }

/** Статус модерации записи */
export type ModerationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/** Статус верификации (публичный) */
export type VerificationStatus = 'verified' | 'pending'

/** Статус документа */
export type DocumentStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/** Прикреплённый документ (PDF, изображение и т.д.) */
export interface ArchiveDocument {
  id: number
  originalFileName: string
  fileType: string
  fileSize: number
  status: DocumentStatus
  uploadedByUsername: string
  uploadedAt: string
  moderatedByUsername: string | null
  moderatedAt: string | null
  rejectionReason: string | null
  fileUrl: string
}

/** Основная модель репрессированного */
export interface Person {
  id: PersonId
  fullName: string
  birthYear: number
  deathYear: number | null
  region: string
  district: string
  occupation: string
  charge: string
  accusation: string       // Алиас charge для отображения в UI
  arrestDate: string | null
  sentence: string
  sentenceDate: string | null
  rehabilitationDate: string | null
  biography: string
  source: string
  status: string
  moderationStatus: ModerationStatus
  createdByEmail: string
  createdAt: string
  rejectionReason: string | null
  documents: ArchiveDocument[]
}

/** 
 * Полная модель записи для модерации.
 * Теперь Person уже содержит все поля, ModerationPerson — алиас для совместимости.
 */
export type ModerationPerson = Person

/** DTO для создания/обновления записи */
export interface PersonCreateDto {
  fullName: string
  birthYear: number
  deathYear?: number | null
  region: string
  district: string
  occupation: string
  charge: string           // Мы маппим accusation из UI в charge бэкенда
  arrestDate?: string | null
  sentence: string
  sentenceDate?: string | null
  rehabilitationDate?: string | null
  biography: string
  source: string
}

/** DTO для формы добавления записи (строковые поля — до валидации) */
export interface PersonFormData {
  fullName: string
  birthYear: string
  deathYear: string
  region: string
  district: string
  occupation: string
  accusation: string       // Мапится в charge
  arrestDate: string
  sentence: string
  sentenceDate: string
  rehabilitationDate: string
  biography: string
  source: string           // Название дела
}

/** Параметры фильтрации для списка / поиска */
export interface PersonFilters {
  query?: string          // Текст поиска
  search?: string         // (Legacy / Alias для query)
  region?: string         // Область
  year?: string | number  // Год
  charge?: string         // Статья
  status?: string
  moderationStatus?: ModerationStatus
  decade?: string
  accusationType?: string
}
