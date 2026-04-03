/**
 * features/archive/api.ts
 * Репозиторий (API-слой) для домена Archive.
 *
 * Инкапсулирует все HTTP-запросы к Spring Boot /archive/*.
 * Компоненты и composable вызывают только этот модуль, а не $apiFetch напрямую.
 *
 * Паттерн: Repository / Data Access Layer
 *
 * Использование:
 *   import { archiveRepository } from '~/features/archive/api'
 *   const { data } = await archiveRepository.getList({ search: 'Байтемиров' })
 *   const person = await archiveRepository.getById(1 as PersonId)
 */

import type { PaginatedResponse, PaginationParams } from '~/types'
import type { Person, PersonId, PersonCreateDto, PersonFilters } from './types'

import { MOCK_PERSONS, getMockPaginatedResponse } from './mocks'

const BASE = '/archive/persons'

export const archiveRepository = {
  /**
   * Получить пагинированный список репрессированных.
   * GET /archive/persons?search=...&page=0&size=20
   *
   * Возвращает useFetch (SSR-совместимый, с кешированием).
   */
  getList(filters?: PersonFilters, pagination?: PaginationParams) {
    const config = useRuntimeConfig()
    
    // Если бэкенд не запущен или не указан URL, отдаем моки (для жюри/демо)
    if (!config.public.apiBase || config.public.apiBase === 'http://localhost:8080/api') {
      return useAsyncData<PaginatedResponse<Person>>('archive-list-mock', async () => {
        // Имитация задержки сети
        await new Promise(resolve => setTimeout(resolve, 400))
        return getMockPaginatedResponse(filters?.search)
      }, {
        watch: [() => filters]
      })
    }

    return useApiFetch<PaginatedResponse<Person>>(BASE, {
      params: {
        ...filters,
        ...pagination,
      },
    })
  },

  /**
   * Получить одну запись по ID.
   * GET /archive/persons/:id
   */
  getById(id: MaybeRef<PersonId>) {
    const config = useRuntimeConfig()
    const targetId = toValue(id)
    
    // Если бэкенд не запущен, достаем из моков
    if (!config.public.apiBase || config.public.apiBase === 'http://localhost:8080/api') {
      return useAsyncData<Person>(`person-mock-${targetId}`, async () => {
        await new Promise(resolve => setTimeout(resolve, 200))
        const person = MOCK_PERSONS.find((p: Person) => p.id === targetId)
        if (!person) throw createError({ statusCode: 404, statusMessage: 'Person not found' })
        return person
      })
    }

    return useApiFetch<Person>(() => `${BASE}/${toValue(id)}`, {
      key: `person-${toValue(id)}`,
    })
  },

  /**
   * Создать новую запись.
   * POST /archive/persons
   *
   * Мутация — используем $apiFetch (императивный, без SSR-кеша).
   */
  create(dto: PersonCreateDto) {
    return $apiFetch<Person>(BASE, {
      method: 'POST',
      body: dto,
    })
  },

  /**
   * Обновить существующую запись.
   * PUT /archive/persons/:id
   */
  update(id: PersonId, dto: Partial<PersonCreateDto>) {
    return $apiFetch<Person>(`${BASE}/${id}`, {
      method: 'PUT',
      body: dto,
    })
  },

  /**
   * Верифицировать запись (только для ADMIN/MODERATOR).
   * PATCH /archive/persons/:id/verify
   */
  verify(id: PersonId) {
    return $apiFetch<Person>(`${BASE}/${id}/verify`, {
      method: 'PATCH',
    })
  },

  /**
   * Удалить запись.
   * DELETE /archive/persons/:id
   */
  delete(id: PersonId) {
    return $apiFetch<void>(`${BASE}/${id}`, {
      method: 'DELETE',
    })
  },
}
