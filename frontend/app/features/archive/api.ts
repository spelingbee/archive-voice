/**
 * features/archive/api.ts
 * Репозиторий (API-слой) для домена Archive.
 */
import type { PaginatedResponse, PaginationParams } from '~/types'
import type { Person, PersonId, PersonCreateDto, PersonFilters, ModerationPerson } from './types'
import { MOCK_PERSONS, getMockPaginatedResponse } from './mocks'

const BASE = '/persons'

export const archiveRepository = {
  /**
   * Получить пагинированный список репрессированных (базовый метод).
   */
  getList(filters?: PersonFilters, pagination?: PaginationParams) {
    const config = useRuntimeConfig()
    
    if (!config.public.apiBase || config.public.apiBase === 'http://localhost:8080/api') {
      return useAsyncData<PaginatedResponse<Person>>('archive-list-mock', async () => {
        await new Promise(resolve => setTimeout(resolve, 300))
        return getMockPaginatedResponse(filters?.search)
      }, { watch: [() => filters] })
    }

    return useApiFetch<PaginatedResponse<Person>>(BASE, {
      params: { ...filters, ...pagination },
    })
  },

  /**
   * Получить детальную запись по ID (досье).
   */
  getById(id: MaybeRef<PersonId>) {
    return useApiFetch<Person>(() => `${BASE}/${toValue(id)}`, {
      key: `person-${toValue(id)}`,
    })
  },

  /**
   * Создать новую запись (POST /persons).
   */
  create(dto: PersonCreateDto) {
    return $apiFetch<Person>(BASE, { method: 'POST', body: dto })
  },

  /**
   * Обновить существующую запись.
   */
  update(id: PersonId, dto: Partial<PersonCreateDto>) {
    return $apiFetch<Person>(`${BASE}/${id}`, { method: 'PUT', body: dto })
  },

  /**
   * Одобрить запись (модерация).
   */
  approve(id: PersonId) {
    return $apiFetch<void>(`${BASE}/${id}/approve`, { method: 'PATCH' })
  },

  /**
   * Отклонить запись с указанием причины.
   */
  reject(id: PersonId, rejectionReason: string) {
    return $apiFetch<void>(`${BASE}/${id}/reject`, { method: 'PATCH', body: { rejectionReason } })
  },

  /**
   * Загрузить архивный документ (Multipart File).
   */
  uploadDocument(file: File, personId: number, description: string = '') {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('personId', personId.toString())
    formData.append('description', description)

    return $apiFetch('/documents/upload', {
      method: 'POST',
      body: formData,
    })
  },

  /**
   * Реактивный поиск по архиву с фильтрацией (query, region, year, charge).
   * Оборачиваем в computed, чтобы useFetch корректно следил за изменениями всех полей.
   */
  search(params: MaybeRefOrGetter<any>) {
    return useApiFetch<PaginatedResponse<Person>>(`${BASE}/search`, {
      params: computed(() => toValue(params)),
    })
  },

  /**
   * Получить список заявок на модерацию (Admin/Moderator).
   */
  getModerationList(pagination?: PaginationParams) {
    return useApiFetch<PaginatedResponse<ModerationPerson>>(`${BASE}/moderation`, {
      params: pagination,
    })
  },

  /**
   * Получить список собственных добавленных записей текущего пользователя.
   */
  getMyList(pagination?: PaginationParams) {
    return useApiFetch<PaginatedResponse<Person>>(`${BASE}/my`, {
      params: pagination,
    })
  },

  /**
   * Получить URL для скачивания документа.
   * Используется в <a href="..."> и для программного скачивания.
   */
  getDocumentDownloadUrl(documentId: number): string {
    const config = useRuntimeConfig()
    return `${config.public.apiBase}/documents/${documentId}/download`
  },

  /**
   * Скачать документ программно (с авторизацией).
   * GET /api/documents/{id}/download
   */
  async downloadDocument(documentId: number, fileName: string) {
    const config = useRuntimeConfig()
    const auth = useAuth()
    const url = `${config.public.apiBase}/documents/${documentId}/download`

    const response = await fetch(url, {
      credentials: 'include',
      headers: {
        ...(auth.token.value ? { Authorization: `Bearer ${auth.token.value}` } : {}),
      },
    })

    if (!response.ok) throw new Error(`Download failed: ${response.status}`)

    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(blobUrl)
  },
}
