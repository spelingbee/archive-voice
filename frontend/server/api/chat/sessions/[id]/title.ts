/**
 * server/api/chat/sessions/[id]/title.ts
 * Получение / изменение заголовка сессии.
 */
export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id');
  const method = getMethod(event); // GET or PATCH/PUT
  const body = method !== 'GET' ? await readBody(event) : null;
  const config = useRuntimeConfig();
  const token = getCookie(event, 'auth_token');

  const response = await $fetch.raw(`${config.public.apiBase}/chat/sessions/${id}/title`, {
    method,
    headers: {
      ...(token && { 'Authorization': `Bearer ${token}` }),
    },
    body,
  });

  return response._data;
});
