/**
 * server/api/chat/sessions.get.ts
 * Получение истории чатов (сессий).
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig();
  const token = getCookie(event, 'auth_token');

  const response = await $fetch.raw(`${config.public.apiBase}/chat/sessions`, {
    headers: {
      ...(token && { 'Authorization': `Bearer ${token}` }),
    },
  });

  return response._data;
});
