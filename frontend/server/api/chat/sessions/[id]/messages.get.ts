/**
 * server/api/chat/sessions/[id]/messages.get.ts
 * Получение сообщений конкретной сессии.
 */
export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id');
  const config = useRuntimeConfig();
  const token = getCookie(event, 'auth_token');

  const response = await $fetch.raw(`${config.public.apiBase}/chat/sessions/${id}/messages`, {
    headers: {
      ...(token && { 'Authorization': `Bearer ${token}` }),
    },
  });

  return response._data;
});
