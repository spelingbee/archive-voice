/**
 * server/api/tts.post.ts
 * Прокси-сервер для озвучки текста через OpenAI TTS.
 */
export default defineEventHandler(async (event) => {
  const body = await readBody(event);
  const config = useRuntimeConfig();

  if (!config.openaiKey) {
    throw createError({ 
      statusCode: 500, 
      statusMessage: 'OpenAI API Key is not configured (NUXT_OPENAI_KEY)' 
    });
  }

  // Делаем запрос к OpenAI
  const response = await fetch('https://api.openai.com/v1/audio/speech', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${config.openaiKey}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      model: 'tts-1',
      voice: 'onyx', // 'onyx' - строгий мужской диктор
      input: body.text,
      speed: 0.95 // Чуть замедляем для драматичности
    }),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw createError({ 
      statusCode: response.status, 
      statusMessage: errorData.error?.message || 'TTS Failed' 
    });
  }

  // Возвращаем аудиопоток
  const arrayBuffer = await response.arrayBuffer();
  return Buffer.from(arrayBuffer);
});
