/**
 * types/auth.ts
 * Типы для авторизации и ролей пользователя.
 * Роли маппируются на Spring Security roles.
 */

/** Роли пользователя в системе */
export type UserRole = 'ADMIN' | 'MODERATOR' | 'RESEARCHER' | 'USER'

/** Профиль авторизованного пользователя */
export interface AuthUser {
  id: number
  email: string
  fullName: string
  role: UserRole
}

/** Данные для входа */
export interface LoginCredentials {
  email: string
  password: string
}

/** Данные для регистрации */
export interface RegisterPayload {
  email: string
  password: string
  fullName: string
}

/** Ответ на успешную аутентификацию */
export interface AuthResponse {
  token: string
  user: AuthUser
}
