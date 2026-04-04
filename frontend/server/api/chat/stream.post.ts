/**
 * server/api/chat/stream.post.ts
 * Прокси-сервер для SSE-стриминга ИИ-чата.
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig();
  const token = getCookie(event, 'auth_token');
  const body = await readBody(event);

  // Формируем payload для бэкенда (поддержка camelCase по требованию)
  const backendPayload = {
    question: body.question,
    personId: body.personId || null,
    sessionId: body.sessionId || null,
    stream: true // Указываем бэкенду, что нужен стриминг
  };

  // Проксируем запрос на бэкенд вручную для поддержки стриминга и трансформации тела запроса
  const response = await $fetch.raw(`${config.public.apiBase}/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` }),
    },
    body: backendPayload,
    responseType: 'stream'
  });

  // Устанавливаем заголовки ответа (SSE)
  setResponseHeader(event, 'Content-Type', 'text/event-stream');
  setResponseHeader(event, 'Cache-Control', 'no-cache');
  setResponseHeader(event, 'Connection', 'keep-alive');

  return sendStream(event, response._data);
});
