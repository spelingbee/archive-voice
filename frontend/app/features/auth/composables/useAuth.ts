/**
 * features/auth/composables/useAuth.ts
 * Composable авторизации — единый источник правды о текущем пользователе.
 *
 * Использует useState (SSR-safe глобальный стейт) для хранения пользователя.
 * Предоставляет:
 *  - Реактивные computed-свойства: isAuthenticated, isAdmin, isModerator
 *  - Методы: login(), logout(), fetchCurrentUser()
 *
 * Использование:
 *   const { user, isAdmin, login, logout } = useAuth()
 */

import type { AuthUser, LoginCredentials, RegisterPayload, AuthResponse } from '~/types'

export function useAuth() {
  // --- State ---
  // useState обеспечивает SSR-гидратацию и глобальный синглтон
  const user = useState<AuthUser | null>('auth:user', () => null)
  const token = useCookie('auth_token', {
    maxAge: 60 * 60 * 24 * 7, // 7 дней
    sameSite: 'lax',
  })

  // --- Computed (роли) ---
  const isAuthenticated = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isModerator = computed(
    () => user.value?.role === 'MODERATOR' || isAdmin.value,
  )
  const isResearcher = computed(
    () => user.value?.role === 'RESEARCHER' || isModerator.value,
  )

  // --- Actions ---

  /** Вход в систему: POST /auth/login */
  async function login(credentials: LoginCredentials): Promise<void> {
    const response = await $apiFetch<AuthResponse>('/auth/login', {
      method: 'POST',
      body: credentials,
    })

    // Сохраняем токен в куку (автоматически будет подхвачен useApiFetch)
    token.value = response.token
    user.value = response.user
  }

  /** Регистрация: POST /auth/register */
  async function register(payload: RegisterPayload): Promise<void> {
    const response = await $apiFetch<AuthResponse>('/auth/register', {
      method: 'POST',
      body: payload,
    })

    token.value = response.token
    user.value = response.user
  }

  /** Выход: очистка стейта и куки */
  function logout(): void {
    user.value = null
    token.value = null
    navigateTo('/auth')
  }

  /** Получить профиль текущего пользователя (при инициализации приложения) */
  async function fetchCurrentUser(): Promise<void> {
    if (!token.value) return

    try {
      const response = await $apiFetch<AuthUser>('/auth/me')
      user.value = response
    } catch {
      // Токен невалиден — сбрасываем
      token.value = null
      user.value = null
    }
  }

  return {
    // State
    user: readonly(user),
    token: readonly(token),

    // Computed
    isAuthenticated,
    isAdmin,
    isModerator,
    isResearcher,

    // Actions
    login,
    register,
    logout,
    fetchCurrentUser,
  }
}
