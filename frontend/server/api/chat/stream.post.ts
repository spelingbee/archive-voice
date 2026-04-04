/**
 * server/api/chat/stream.post.ts
 * Прокси-сервер для SSE-стриминга ИИ-чата.
 * Используем нативный fetch (Node 18+) для правильного стриминга.
 */
export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig();
  const token = getCookie(event, 'auth_token');
  const body = await readBody(event);

  const backendPayload = {
    question: body.question,
    personId: body.personId || null,
    sessionId: body.sessionId || null,
  };

  // Нативный fetch для правильного стриминга (ofetch/$fetch буферизует ответ)
  const response = await fetch(`${config.public.apiBase}/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
      ...(token && { 'Authorization': `Bearer ${token}` }),
    },
    body: JSON.stringify(backendPayload),
  });

  if (!response.ok || !response.body) {
    throw createError({
      statusCode: response.status,
      statusMessage: 'Ошибка SSE стриминга',
    });
  }

  // SSE заголовки
  setResponseHeader(event, 'Content-Type', 'text/event-stream');
  setResponseHeader(event, 'Cache-Control', 'no-cache');
  setResponseHeader(event, 'Connection', 'keep-alive');
  setResponseHeader(event, 'X-Accel-Buffering', 'no');

  // Пайпим ReadableStream напрямую
  return sendStream(event, response.body);
});