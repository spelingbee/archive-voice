/**
 * composables/useApiFetch.ts
 * Базовый API-клиент для всего приложения.
 */

import type { UseFetchOptions } from 'nuxt/app'
import { ofetch } from 'ofetch'

/**
 * SSR-совместимый composable для GET-запросов с кешированием.
 */
export function useApiFetch<T>(
  url: string | (() => string),
  options: UseFetchOptions<T> = {},
) {
  const config = useRuntimeConfig()
  const auth = useAuth()

  return useFetch(url, {
    baseURL: config.public.apiBase as string,
    credentials: 'include',

    // --- Инцептор запроса ---
    onRequest({ options: reqOptions }) {
      const token = auth.token.value
      if (token) {
        const headers = (reqOptions.headers || {}) as any
        if (headers instanceof Headers) {
          headers.set('Authorization', `Bearer ${token}`)
        } else if (Array.isArray(headers)) {
          headers.push(['Authorization', `Bearer ${token}`])
        } else {
          headers.Authorization = `Bearer ${token}`
        }
        reqOptions.headers = headers
      }
    },

    // --- Автоматическая распаковка { data: T } ---
    transform: (res: any) => res.data ?? res,

    // --- Обработка 401 — просим useAuth обновиться ---
    async onResponseError({ response }) {
      if (response.status === 401) {
        const urlStr = typeof url === 'function' ? url() : url
        const isAuthRoute = urlStr.includes('/auth/login') || urlStr.includes('/auth/register') || urlStr.includes('/auth/refresh')
        
        if (!isAuthRoute && import.meta.client) {
          try {
            await auth.refresh()
          } catch {
            auth.logout()
          }
        }
      }
    },

    ...options,
  })
}

/**
 * Императивный клиент для мутаций (POST, PUT, DELETE).
 */
export async function $apiFetch<T>(
  url: string,
  options: any = {},
): Promise<T> {
  const config = useRuntimeConfig()
  const auth = useAuth()

  // Вспомогательная функция для системной распаковки { data: T } -> T
  const extractData = (res: any) => res?.data ?? res

  const fetchOptions = {
    baseURL: config.public.apiBase as string,
    credentials: 'include' as const,
    ...options,
    headers: {
      ...options.headers,
      ...(auth.token.value ? { Authorization: `Bearer ${auth.token.value}` } : {}),
    },
  }

  try {
    const response = await ofetch(url, fetchOptions)
    return extractData(response) as T
  } catch (err: any) {
    if (err.response?.status === 401) {
      const isAuthRoute = url.includes('/auth/login') || url.includes('/auth/register') || url.includes('/auth/refresh')
      
      if (!isAuthRoute && import.meta.client) {
        try {
          await auth.refresh()
          
          const freshToken = auth.token.value
          const retryResponse = await ofetch(url, {
            ...fetchOptions,
            headers: {
              ...fetchOptions.headers,
              Authorization: `Bearer ${freshToken}`,
            },
          })
          
          return extractData(retryResponse) as T
        } catch (refreshErr) {
          auth.logout()
          throw refreshErr
        }
      }
    }
    throw err
  }
}
