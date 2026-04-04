/**
 * server/api/chat.post.ts
 * Прокси-сервер для отправки сообщений в ИИ-чат.
 */
export default defineEventHandler(async (event) => {
  const body = await readBody(event);
  const config = useRuntimeConfig();
  const token = getCookie(event, 'auth_token');

  // Формируем payload для бэкенда по требованию:
  // person_id и session_id не обязательны, но если их нет — прокидываем null.
  const backendPayload = {
    question: body.question,
    personId: body.personId || null,
    sessionId: body.sessionId || null,
  };

  /**
   * Примечание: Для полноценного SSE-стриминга здесь потребовался бы
   * специальный обработчик. Пока реализуем как обычный POST-запрос
   * по просьбе пользователя.
   */
  const response = await $fetch.raw(`${config.public.apiBase}/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` }),
    },
    body: backendPayload,
  });

  return response._data;
});
