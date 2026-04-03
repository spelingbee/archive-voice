<script setup lang="ts">
/**
 * features/archive/components/ContextChat.vue
 * Контекстный ИИ-чат для конкретного дела.
 *
 * Отличие от глобального ChatSlideOver:
 * — привязан к personId → RAG ищет только в документах этого дела
 * — встроен inline в карточку, не slide-over
 * — использует useChatStream для реального стриминга
 */
import type { ChatMessage } from '~/features/chat/types'

const props = defineProps<{
  /** ID персоны для контекстного RAG-поиска */
  personId?: number
  /** Заголовок чата */
  title?: string
  /** Подзаголовок */
  subtitle?: string
}>()

// --- SSE стриминг ---
const {
  currentResponse,
  isStreaming,
  isConnecting,
  sendMessage,
  abort,
  error: streamError,
} = useChatStream()

const inputMessage = ref('')
const messages = ref<ChatMessage[]>([])
const messagesContainer = ref<HTMLElement | null>(null)

let messageCounter = 0
const nextId = () => `ctx-${++messageCounter}`

/** Автопрокрутка к последнему сообщению */
function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

watch(currentResponse, () => scrollToBottom())

async function handleSubmit() {
  const prompt = inputMessage.value.trim()
  if (!prompt || isStreaming.value) return

  // Добавляем сообщение пользователя
  messages.value.push({
    id: nextId(),
    role: 'user',
    content: prompt,
    timestamp: new Date(),
  })

  inputMessage.value = ''
  scrollToBottom()

  // Стриминг ответа с контекстом персоны
  await sendMessage(prompt, props.personId)

  // Сохраняем ответ в историю
  if (currentResponse.value) {
    messages.value.push({
      id: nextId(),
      role: 'assistant',
      content: currentResponse.value,
      timestamp: new Date(),
    })
  }
}
</script>

<template>
  <div class="flex flex-col h-full bg-white border border-border">
    <!-- Header -->
    <header class="px-4 py-3 border-b border-border">
      <h3
        class="text-base font-semibold text-ink"
        style="font-family: var(--font-serif)"
      >
        {{ title || 'Спросить ИИ' }}
      </h3>
      <p v-if="subtitle" class="text-xs text-ink-muted mt-0.5">
        {{ subtitle }}
      </p>
      <p v-if="personId" class="text-[10px] text-ink-muted mt-1" style="font-family: var(--font-mono)">
        Контекст: дело #{{ personId }}
      </p>
    </header>

    <!-- Messages -->
    <div ref="messagesContainer" class="flex-1 overflow-y-auto min-h-0">
      <div class="divide-y divide-border">
        <article
          v-for="message in messages"
          :key="message.id"
          class="px-4 py-3"
        >
          <header class="mb-1.5">
            <h4
              v-if="message.role === 'user'"
              class="text-xs font-bold text-ink uppercase tracking-wide"
            >
              Исследователь
            </h4>
            <h4
              v-else
              class="text-xs font-bold text-ink-secondary uppercase tracking-wide"
            >
              Система (RAG)
            </h4>
          </header>
          <p class="text-sm text-ink leading-relaxed whitespace-pre-wrap">
            {{ message.content }}
          </p>
          <footer v-if="message.source" class="mt-2">
            <span
              class="inline-flex items-center px-1.5 py-0.5 text-[10px] text-ink-muted bg-paper-dark"
              style="font-family: var(--font-mono)"
            >
              Источник: {{ message.source }}
            </span>
          </footer>
        </article>

        <!-- Текущий стриминг (typewriter) -->
        <article v-if="isStreaming || isConnecting" class="px-4 py-3">
          <header class="mb-1.5">
            <h4 class="text-xs font-bold text-ink-secondary uppercase tracking-wide">
              Система (RAG)
            </h4>
          </header>
          <p v-if="isConnecting && !currentResponse" class="text-sm text-ink-muted animate-pulse">
            Поиск по документам дела...
          </p>
          <p v-else class="text-sm text-ink leading-relaxed whitespace-pre-wrap">
            {{ currentResponse }}<span class="animate-pulse">▍</span>
          </p>
        </article>
      </div>

      <!-- Пустое состояние -->
      <div
        v-if="messages.length === 0 && !isStreaming"
        class="flex items-center justify-center h-full text-center px-6 py-12"
      >
        <p class="text-sm text-ink-muted">
          Задайте вопрос о деле — ИИ ответит на основе архивных документов.
        </p>
      </div>
    </div>

    <!-- Ошибка -->
    <div v-if="streamError" class="px-4 py-2 bg-red-50 border-t border-red-200">
      <p class="text-xs text-red-700">{{ streamError }}</p>
    </div>

    <!-- Input Area -->
    <footer class="border-t border-border p-3">
      <form class="flex gap-2" @submit.prevent="handleSubmit">
        <label for="context-chat-input" class="sr-only">Введите вопрос</label>
        <input
          id="context-chat-input"
          v-model="inputMessage"
          type="text"
          placeholder="Задайте вопрос о деле..."
          class="flex-1 px-3 py-2 text-sm bg-white border border-border focus:border-ink focus:outline-none transition-colors"
          :disabled="isStreaming"
        />
        <button
          v-if="isStreaming"
          type="button"
          class="px-3 py-2 bg-accent text-white text-sm hover:bg-accent-hover transition-colors"
          aria-label="Остановить"
          @click="abort"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="6" y="6" width="12" height="12" rx="2" />
          </svg>
        </button>
        <button
          v-else
          type="submit"
          class="px-3 py-2 bg-ink text-white text-sm hover:bg-ink/90 transition-colors"
          aria-label="Отправить"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M5 12h14" />
            <path d="m12 5 7 7-7 7" />
          </svg>
        </button>
      </form>
    </footer>
  </div>
</template>
