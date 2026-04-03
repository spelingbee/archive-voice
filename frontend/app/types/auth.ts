/**
 * types/auth.ts
 * Типы для авторизации и ролей пользователя.
 * Роли маппируются на Spring Security roles.
 */

/** Роли пользователя в системе */
export type UserRole = 'ADMIN' | 'MODERATOR' | 'RESEARCHER' | 'USER'

/** Профиль авторизованного пользователя (согласно ответу бэкенда) */
export interface AuthUser {
  id: number
  email: string
  firstName: string
  lastName: string
  role: UserRole
  createdAt?: string
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
  firstName: string
  lastName: string
}

/** 
 * Ответ на успешную аутентификацию (Login / Register / Refresh).
 * Примечание: Глобальный интерцептор useApiFetch автоматически распаковывает 
 * поле { data: T } -> T, поэтому здесь описываем внутреннюю структуру.
 */
export interface AuthResponse {
  accessToken: string
  refreshToken: string | null
  type: string | null
  user: AuthUser
}
