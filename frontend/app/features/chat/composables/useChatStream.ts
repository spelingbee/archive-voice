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

import type { ChatMessage, ChatRole, ChatSession, StreamStatus } from '../types'

export function useChatStream() {
  const config = useRuntimeConfig()
  const token = useCookie('auth_token')

  // --- Глобальное реактивное состояние (Nuxt useState) ---
  /** Текущие сообщения в активном чате */
  const messages = useState<ChatMessage[]>('chat_messages', () => [])
  /** Текущий ответ ИИ (при загрузке) */
  const currentResponse = useState<string>('chat_current_response', () => '')
  /** ID текущей сессии чата */
  const sessionId = useState<string | null>('chat_session_id', () => null)
  /** Статус запроса */
  const status = useState<StreamStatus>('chat_status', () => 'idle')
  /** Ошибка (если произошла) */
  const error = useState<string | null>('chat_error', () => null)
  /** Список источников для последнего ответа */
  const lastSources = useState<any[]>('chat_last_sources', () => [])
  
  /** Список всех сессий чата (история) */
  const sessions = useState<ChatSession[]>('chat_sessions_list', () => [])
  /** Сообщения загруженной из истории сессии (для превью) */
  const sessionMessages = useState<ChatMessage[]>('chat_session_messages', () => [])

  /**
   * Отправить сообщение с поддержкой стриминга.
   *
   * @param question - Текст вопроса
   * @param personId - ID персоны (необязательно)
   */
  async function sendMessage(question: string, personId?: number): Promise<void> {
    currentResponse.value = ''
    error.value = null
    status.value = 'streaming' // Уже не просто connecting, а стриминг
    lastSources.value = []

    // Добавляем сообщение пользователя в историю
    messages.value.push({
      id: `u-${Date.now()}`,
      role: 'user',
      content: question,
      timestamp: new Date()
    })

    try {
      const response = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          question,
          personId: personId || null,
          sessionId: sessionId.value,
        })
      })

      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)

      const reader = response.body?.getReader()
      if (!reader) throw new Error('ReadableStream not supported')

      const decoder = new TextDecoder()
      let buffer = ''
      let fullText = ''

      // Читаем поток чанками
      while (true) {
        const { value, done } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // SSE события разделены двойным \n\n
        const parts = buffer.split('\n\n')
        buffer = parts.pop() || '' // последний кусок может быть неполным

        for (const part of parts) {
          const line = part.trim()
          if (!line.startsWith('data:')) continue

          const json = line.slice(5).trim()
          try {
            const eventData = JSON.parse(json)

            switch (eventData.event) {
              case 'token':
                // Текст прилетает посимвольно в content
                if (eventData.content) {
                  fullText += eventData.content
                  currentResponse.value = fullText
                }
                break
                
              case 'meta':
                // Метадата: session_id + источники
                if (eventData.session_id || eventData.sessionId) {
                  sessionId.value = String(eventData.session_id || eventData.sessionId)
                }
                if (eventData.sources) {
                  lastSources.value = eventData.sources
                }
                break

              case 'done':
                // Поток завершен
                if (eventData.session_id || eventData.sessionId) {
                   sessionId.value = String(eventData.session_id || eventData.sessionId)
                }
                break
            }
          } catch (e) {
            // Неполный JSON или ошибка парсинга — пропускаем
          }
        }
      }

      // После завершения — добавляем ответ ассистента в историю
      if (currentResponse.value) {
        messages.value.push({
          id: `a-${Date.now()}`,
          role: 'assistant',
          content: currentResponse.value,
          source: lastSources.value?.[0]?.filename,
          timestamp: new Date()
        })
      }
      
      status.value = 'done'
    } catch (err: any) {
      status.value = 'error'
      error.value = err.message || 'Ошибка стриминга чата'
    }
  }

  /**
   * Загрузить список сессий (историю) чатов.
   */
  async function fetchSessions(): Promise<void> {
    status.value = 'connecting'
    try {
      const response = await $fetch<any>('/api/chat/sessions')
      if (response && response.data) {
        sessions.value = response.data.map((s: any) => ({
          id: String(s.id),
          date: s.created_at || s.updated_at || new Date().toISOString(),
          firstQuestion: s.title || '...',
          messagesCount: s.message_count || 0
        }))
      }
      status.value = 'done'
    } catch (err: any) {
      status.value = 'error'
      // Не блокируем UI при ошибке загрузки истории
    }
  }

  /**
   * Загрузить сообщения конкретной сессии и сделать её активной.
   */
  async function loadSession(id: string): Promise<void> {
    status.value = 'connecting'
    messages.value = []
    sessionId.value = id
    
    try {
      const response = await $fetch<any>(`/api/chat/sessions/${id}/messages`)
      if (response && response.data) {
        messages.value = response.data.map((m: any) => ({
          id: String(m.id),
          role: m.role as ChatRole,
          content: m.content,
          timestamp: new Date(m.created_at),
          source: m.sources?.[0]?.filename || m.source
        }))
      }
      status.value = 'done'
    } catch (err: any) {
      status.value = 'error'
      error.value = 'Ошибка загрузки истории сообщений'
    }
  }

  /**
   * Загрузить сообщения для превью (например, в дашборде).
   */
  async function fetchSessionMessages(id: string): Promise<void> {
    sessionMessages.value = []
    try {
      const response = await $fetch<any>(`/api/chat/sessions/${id}/messages`)
      if (response && response.data) {
        sessionMessages.value = response.data.map((m: any) => ({
          id: String(m.id),
          role: m.role as ChatRole,
          content: m.content,
          timestamp: new Date(m.created_at),
          source: m.sources?.[0]?.filename || m.source
        }))
      }
    } catch (err: any) {}
  }

  /**
   * Обновить заголовок сессии.
   */
  async function updateSessionTitle(sessionId: string, title: string): Promise<void> {
    try {
      await $fetch(`/api/chat/sessions/${sessionId}/title`, {
        method: 'PATCH',
        body: { title }
      })
      if (sessions.value) {
        const idx = sessions.value.findIndex(s => s.id === sessionId)
        if (idx !== -1) sessions.value[idx].firstQuestion = title
      }
    } catch (err: any) {}
  }

  /** Сбросить состояние (начать новый чат) */
  function reset(): void {
    sessionId.value = null
    messages.value = []
    currentResponse.value = ''
    status.value = 'idle'
    error.value = null
    lastSources.value = []
  }

  // Computed-хелперы для UI
  const isStreaming = computed(() => status.value === 'streaming')
  const isConnecting = computed(() => status.value === 'connecting')
  const isIdle = computed(() => status.value === 'idle')

  return {
    // State
    messages,
    currentResponse: readonly(currentResponse),
    sessionId: readonly(sessionId),
    sessions: readonly(sessions),
    sessionMessages: readonly(sessionMessages),
    status: readonly(status),
    error: readonly(error),
    lastSources: readonly(lastSources),

    // Computed
    isStreaming,
    isConnecting,
    isIdle,

    // Actions
    sendMessage,
    fetchSessions,
    loadSession,
    fetchSessionMessages,
    updateSessionTitle,
    reset,
  }
}
