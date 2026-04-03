/**
 * composables/useApiFetch.ts
 * Базовый API-клиент для всего приложения.
 *
 * Оборачивает нативный Nuxt `useFetch` / `$fetch` с:
 * 1. Автоподмешиванием JWT токена (Bearer) из куки `auth_token`
 * 2. Автоматическим baseURL из runtimeConfig
 * 3. Перехватом 401 → редирект на /auth и очисткой токена
 * 4. Единообразной обработкой ошибок
 *
 * Использование:
 *   const { data, error } = await useApiFetch<Person[]>('/archive/persons')
 *   const result = await $apiFetch<Person>('/archive/persons/1')
 */

import type { UseFetchOptions } from 'nuxt/app'

/**
 * SSR-совместимый composable для GET-запросов с кешированием.
 * Обёртка над useFetch с JWT и обработкой ошибок.
 */
export function useApiFetch<T>(
  url: string | (() => string),
  options: UseFetchOptions<T> = {},
) {
  const config = useRuntimeConfig()
  const token = useCookie('auth_token')

  return useFetch(url, {
    // Базовый URL из runtimeConfig (по умолчанию /api проксируется через Nuxt)
    baseURL: config.public.apiBase as string,

    // --- Interceptors (аналог axios interceptors) ---

    onRequest({ options: reqOptions }) {
      // Подмешиваем JWT Bearer токен, если он есть в куке
      if (token.value) {
        const headers = new Headers(reqOptions.headers)
        headers.set('Authorization', `Bearer ${token.value}`)
        reqOptions.headers = headers
      }
    },

    onResponseError({ response }) {
      // 401 Unauthorized → сброс токена и редирект на страницу авторизации
      if (response.status === 401) {
        token.value = null
        navigateTo('/auth')
      }
    },

    // Позволяем переопределять любые опции
    ...options,
  })
}

/**
 * Императивный клиент для мутаций (POST, PUT, DELETE).
 * НЕ использует SSR-кеширование — вызывается только на клиенте.
 *
 * Использование:
 *   const person = await $apiFetch<Person>('/archive/persons', {
 *     method: 'POST',
 *     body: { fullName: '...' }
 *   })
 */
export function $apiFetch<T>(
  url: string,
  options: Parameters<typeof $fetch>[1] = {},
): Promise<T> {
  const config = useRuntimeConfig()
  const token = useCookie('auth_token')

  return $fetch<T>(url, {
    baseURL: config.public.apiBase as string,

    onRequest({ options: reqOptions }) {
      if (token.value) {
        const headers = new Headers(reqOptions.headers)
        headers.set('Authorization', `Bearer ${token.value}`)
        reqOptions.headers = headers
      }
    },

    onResponseError({ response }) {
      if (response.status === 401) {
        token.value = null
        navigateTo('/auth')
      }
    },

    ...options,
  })
}
