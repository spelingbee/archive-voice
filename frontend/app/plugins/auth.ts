/**
 * plugins/auth.ts
 * Плагин для инициализации состояния авторизации при загрузке приложения.
 * Работает и на сервере (SSR), и на клиенте.
 */

export default defineNuxtPlugin(async (nuxtApp) => {
  const { fetchCurrentUser } = useAuth()

  // Пытаемся получить профиль текущего пользователя при инициализации
  // Это восстановит состояние из куки `auth_token` после перезагрузки
  await fetchCurrentUser()
})
