/**
 * features/chat/composables/useChatStream.ts
 * Composable для RAG ИИ-чата с SSE-стримингом.
 *
 * Реализует Server-Sent Events через ReadableStream (native fetch).
 * $fetch / useFetch НЕ поддерживают стриминг — поэтому используем
 * нативный fetch + TextDecoderStream для чтения чанков.
 *
 * Эффект:
 *   Текст появляется посимвольно / по чанкам (typewriter effect),
 *   реактивно обновляя ref `currentResponse`.
 *
 * Использование:
 *   const { currentResponse, status, error, sendMessage, abort } = useChatStream()
 *   await sendMessage('Что известно о Байтемирове?')
 *   // currentResponse.value обновляется в реальном времени
 */

import type { StreamStatus } from '../types'

export function useChatStream() {
  const config = useRuntimeConfig()
  const token = useCookie('auth_token')

  // --- Реактивное состояние ---
  /** Текущий ответ ИИ (обновляется по мере стриминга) */
  const currentResponse = ref('')
  /** Статус стриминга */
  const status = ref<StreamStatus>('idle')
  /** Ошибка (если произошла) */
  const error = ref<string | null>(null)
  /** AbortController для отмены запроса */
  let abortController: AbortController | null = null

  /**
   * Отправить сообщение и начать стриминг ответа.
   *
   * @param prompt - Текст вопроса пользователя
   * @param documentId - (опционально) ID документа для контекстного RAG-поиска
   */
  async function sendMessage(prompt: string, documentId?: number): Promise<void> {
    // Сброс состояния
    currentResponse.value = ''
    error.value = null
    status.value = 'connecting'

    // Создаём AbortController для возможности отмены
    abortController = new AbortController()

    try {
      // --- Нативный fetch (не $fetch!) для работы с ReadableStream ---
      const response = await fetch(
        `${config.public.apiBase}/chat`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            // JWT токен из куки
            ...(token.value && { Authorization: `Bearer ${token.value}` }),
          },
          body: JSON.stringify({ prompt, documentId }),
          signal: abortController.signal,
        },
      )

      // Проверяем HTTP-статус
      if (!response.ok) {
        if (response.status === 401) {
          token.value = null
          navigateTo('/auth')
          return
        }
        throw new Error(`Ошибка сервера: ${response.status}`)
      }

      // Проверяем наличие тела ответа
      if (!response.body) {
        throw new Error('Сервер не вернул поток данных')
      }

      // --- Чтение SSE потока ---
      status.value = 'streaming'

      const reader = response.body
        .pipeThrough(new TextDecoderStream())
        .getReader()

      // Буфер для обработки неполных SSE-строк
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()

        if (done) {
          // Поток завершён
          status.value = 'done'
          break
        }

        // Добавляем новый чанк в буфер
        buffer += value

        // Обрабатываем SSE-формат: каждая строка начинается с "data: "
        // Сообщения разделены двойным переводом строки
        const lines = buffer.split('\n')

        // Последняя строка может быть неполной — оставляем в буфере
        buffer = lines.pop() || ''

        for (const line of lines) {
          // Пустая строка = конец SSE-события
          if (line.trim() === '') continue

          // SSE-формат: "data: ..."
          if (line.startsWith('data: ')) {
            const data = line.slice(6) // убираем "data: "

            // Маркер конца стрима (конвенция Spring WebFlux)
            if (data === '[DONE]') {
              status.value = 'done'
              return
            }

            // Обновляем реактивный ref — Vue автоматически обновит UI
            currentResponse.value += data
          }
        }
      }
    } catch (err: unknown) {
      // AbortError — пользователь отменил запрос
      if (err instanceof DOMException && err.name === 'AbortError') {
        status.value = 'idle'
        return
      }

      status.value = 'error'
      error.value = err instanceof Error ? err.message : 'Неизвестная ошибка'
    } finally {
      abortController = null
    }
  }

  /** Прервать текущий стриминг */
  function abort(): void {
    abortController?.abort()
    abortController = null
  }

  /** Сбросить состояние */
  function reset(): void {
    abort()
    currentResponse.value = ''
    status.value = 'idle'
    error.value = null
  }

  // Computed-хелперы для UI
  const isStreaming = computed(() => status.value === 'streaming')
  const isConnecting = computed(() => status.value === 'connecting')
  const isIdle = computed(() => status.value === 'idle')

  return {
    // State
    currentResponse: readonly(currentResponse),
    status: readonly(status),
    error: readonly(error),

    // Computed
    isStreaming,
    isConnecting,
    isIdle,

    // Actions
    sendMessage,
    abort,
    reset,
  }
}
