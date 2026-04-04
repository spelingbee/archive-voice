/**
 * features/chat/types.ts
 * Типы для RAG ИИ-чата.
 */

/** Роль в чате */
export type ChatRole = 'user' | 'assistant'

/** Сообщение в чате */
export interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  /** Источник из архива (если RAG нашёл документ) */
  source?: string
  /** Временная метка */
  timestamp: Date
}

/** Запрос к чат-API */
export interface ChatRequest {
  prompt: string
  /** ID документа для контекстного поиска (RAG) */
  documentId?: number
}

/** Состояние стриминга */
export type StreamStatus = 'idle' | 'connecting' | 'streaming' | 'done' | 'error'

/** Сессия (диалог) чата — для истории в dashboard */
export interface ChatSession {
  id: string
  date: string
  firstQuestion: string
  messagesCount: number
}
