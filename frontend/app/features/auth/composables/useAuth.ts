/**
 * features/auth/composables/useAuth.ts
 * Composable авторизации — единый источник правды о текущем пользователе.
 *
 * Особенности:
 * - Ротируемый accessToken в куки `auth_token`
 * - Изолированный для каждого запроса Token Refresh (SSR-safe)
 * - Реактивные имена (firstName, lastName, fullName)
 */

import type { AuthUser, LoginCredentials, RegisterPayload, AuthResponse } from '~/types'

export function useAuth() {
  const nuxtApp = useNuxtApp()
  
  // --- State ---
  const user = useState<AuthUser | null>('auth:user', () => null)
  const token = useCookie('auth_token', {
    maxAge: 60 * 60 * 24 * 7, // 7 дней
    sameSite: 'lax',
  })

  // --- Computed (имена и роли) ---
  const isAuthenticated = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isModerator = computed(() => user.value?.role === 'MODERATOR' || isAdmin.value)
  const isResearcher = computed(() => user.value?.role === 'RESEARCHER' || isModerator.value)

  /** Полное имя пользователя для UI */
  const fullName = computed(() => {
    if (!user.value) return ''
    return `${user.value.firstName} ${user.value.lastName}`.trim()
  })

  // --- Actions ---

  /** Вход в систему: POST /auth/login */
  async function login(credentials: LoginCredentials): Promise<void> {
    const response = await $apiFetch<AuthResponse>('/auth/login', {
      method: 'POST',
      body: credentials,
    })

    token.value = response.accessToken
    user.value = response.user
  }

  /** Регистрация: POST /auth/register */
  async function register(payload: RegisterPayload): Promise<void> {
    const response = await $apiFetch<AuthResponse>('/auth/register', {
      method: 'POST',
      body: payload,
    })

    token.value = response.accessToken
    user.value = response.user
  }

  /**
   * Обновление токена: POST /auth/refresh
   * Использует httpOnly cookie на бэкенде.
   */
  async function refresh(): Promise<void> {
    // Рефреш должен происходить только на клиенте, так как httpOnly куки 
    // управляются браузером и могут быть недоступны/неприменимы при SSR.
    if (!import.meta.client) return
    
    if ((nuxtApp as any)._authRefreshPromise) {
       return (nuxtApp as any)._authRefreshPromise
    }

    // Если нет токена и мы не в процессе логина/регистрации, 
    // вероятно, сессия была намеренно завершена.
    if (!token.value) return

    const refreshTask = (async () => {
      try {
        const response = await $apiFetch<AuthResponse>('/auth/refresh', {
          method: 'POST',
        })
        token.value = response.accessToken
        user.value = response.user
      } catch (err) {
        // На сервере навигация выбрасывает исключение, которое лучше не глотать полностью
        logout()
        throw err
      } finally {
        (nuxtApp as any)._authRefreshPromise = null
      }
    })()

    ;(nuxtApp as any)._authRefreshPromise = refreshTask
    return refreshTask
  }

  /** Выход из системы */
  async function logout(): Promise<void> {
    // 1. Сначала очищаем локальное состояние (реактивно)
    user.value = null
    token.value = null
    
    // 2. Делаем запрос на бэкенд, чтобы он стер HTTP-only куки (refresh token)
    try {
      await $apiFetch('/auth/logout', { method: 'POST' })
    } catch (err) {
      // Игнорируем ошибки при логауте, т.к. локально мы уже «вышли»
      console.warn('Backend logout failed or not implemented', err)
    }

    // 3. Редирект
    if (import.meta.client || import.meta.server) {
       navigateTo('/auth')
    }
  }

  /** Инициализация пользователя (при загрузке страницы) */
  async function fetchCurrentUser(): Promise<void> {
    if (!token.value) return

    try {
      // Глобальная распаковка { data: T } через useApiFetch теперь 
      // работает корректно и для первичных, и для повторных (retry) запросов.
      const response = await $apiFetch<any>('/auth/me')
      user.value = response.user || response
    } catch (err: any) {
      if (err.response?.status === 401) {
        try {
          await refresh()
        } catch {
          logout()
        }
      }
    }
  }

  return {
    user: readonly(user),
    token: readonly(token),
    isAuthenticated,
    isAdmin,
    isModerator,
    isResearcher,
    fullName,
    login,
    register,
    refresh,
    logout,
    fetchCurrentUser,
  }
}
