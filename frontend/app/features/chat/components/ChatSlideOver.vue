<script setup lang="ts">
/**
 * features/chat/components/ChatSlideOver.vue
 * Панель RAG ИИ-чата (slide-over) — рефакторинг.
 *
 * Изменения:
 * — Cтриминг через useChatStream (вместо захардкоженных сообщений)
 * — Реактивная история сообщений
 * — Анимация печати текста
 * — Автопрокрутка к последнему сообщению
 */
import type { ChatMessage } from '../types'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

// --- Chat Stream ---
const {
  currentResponse,
  isStreaming,
  isConnecting,
  sendMessage,
  abort,
  error: streamError,
} = useChatStream()

// --- Локальный стейт ---
const inputMessage = ref('')
const messages = ref<ChatMessage[]>([])
const messagesContainer = ref<HTMLElement | null>(null)

/** Генерация уникального ID для сообщений */
let messageCounter = 0
const nextId = () => `msg-${++messageCounter}`

/** Автопрокрутка к последнему сообщению */
function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

/** Следим за обновлением стриминга — прокручиваем к низу */
watch(currentResponse, () => {
  scrollToBottom()
})

/** Обработка отправки сообщения */
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

  // Запускаем стриминг ответа ИИ
  await sendMessage(prompt)

  // После завершения стриминга — сохраняем ответ в историю
  if (currentResponse.value) {
    messages.value.push({
      id: nextId(),
      role: 'assistant',
      content: currentResponse.value,
      // source можно парсить из ответа, если бэкенд отдаёт в мета-данных
      timestamp: new Date(),
    })
  }
}

/** Закрытие панели */
function handleClose() {
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <!-- Backdrop -->
    <Transition
      enter-active-class="transition-opacity duration-300 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-200 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="open"
        class="fixed inset-0 bg-ink/20 z-50"
        aria-hidden="true"
        @click="handleClose"
      />
    </Transition>

    <!-- Panel -->
    <Transition
      enter-active-class="transition-transform duration-300 ease-out"
      enter-from-class="translate-x-full"
      enter-to-class="translate-x-0"
      leave-active-class="transition-transform duration-200 ease-in"
      leave-from-class="translate-x-0"
      leave-to-class="translate-x-full"
    >
      <aside
        v-if="open"
        class="fixed top-14 bottom-0 right-0 z-[60] w-full max-w-lg bg-white border-l border-border flex flex-col"
        role="dialog"
        aria-modal="true"
        aria-labelledby="chat-title"
      >
        <!-- Header -->
        <header class="flex items-center justify-between px-5 py-4 border-b border-border">
          <div>
            <h2
              id="chat-title"
              class="text-lg font-semibold text-ink"
              style="font-family: var(--font-serif)"
            >
              Консультация с архивом
            </h2>
            <p class="text-xs text-ink-muted mt-0.5">
              Система поиска по документам (RAG)
            </p>
          </div>
          <button
            type="button"
            class="p-2 text-ink-muted hover:text-ink transition-colors"
            aria-label="Закрыть панель"
            @click="handleClose"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6 6 18" />
              <path d="m6 6 12 12" />
            </svg>
          </button>
        </header>

        <!-- Messages -->
        <div ref="messagesContainer" class="flex-1 overflow-y-auto">
          <div class="divide-y divide-border">
            <!-- История сообщений -->
            <article
              v-for="message in messages"
              :key="message.id"
              class="px-5 py-4"
            >
              <header class="mb-2">
                <h3
                  v-if="message.role === 'user'"
                  class="text-sm font-bold text-ink"
                >
                  Исследователь
                </h3>
                <h3
                  v-else
                  class="text-sm font-bold text-ink-secondary"
                >
                  Система (RAG)
                </h3>
              </header>
              <p class="text-sm text-ink leading-relaxed whitespace-pre-wrap">
                {{ message.content }}
              </p>
              <footer v-if="message.source" class="mt-3">
                <span
                  class="inline-flex items-center px-2 py-1 text-xs text-ink-muted bg-paper-dark"
                  style="font-family: var(--font-mono)"
                >
                  Источник: {{ message.source }}
                </span>
              </footer>
            </article>

            <!-- Текущий стриминг ответа (typewriter) -->
            <article
              v-if="isStreaming || isConnecting"
              class="px-5 py-4"
            >
              <header class="mb-2">
                <h3 class="text-sm font-bold text-ink-secondary">
                  Система (RAG)
                </h3>
              </header>
              <p v-if="isConnecting && !currentResponse" class="text-sm text-ink-muted animate-pulse">
                Поиск по архивным документам...
              </p>
              <p v-else class="text-sm text-ink leading-relaxed whitespace-pre-wrap">
                {{ currentResponse }}<span class="animate-pulse">▍</span>
              </p>
            </article>
          </div>

          <!-- Пустое состояние -->
          <div
            v-if="messages.length === 0 && !isStreaming"
            class="flex flex-col items-center justify-center h-full text-center px-8 py-16"
          >
            <p class="text-sm text-ink-muted">
              Задайте вопрос об архивных записях. ИИ-ассистент найдёт релевантные документы и ответит со ссылками на источники.
            </p>
          </div>
        </div>

        <!-- Ошибка -->
        <div v-if="streamError" class="px-5 py-3 bg-red-50 border-t border-red-200">
          <p class="text-sm text-red-700">
            {{ streamError }}
          </p>
        </div>

        <!-- Input Area -->
        <footer class="border-t border-border p-4">
          <form class="flex gap-3" @submit.prevent="handleSubmit">
            <label for="chat-input" class="sr-only">Введите вопрос</label>
            <input
              id="chat-input"
              v-model="inputMessage"
              type="text"
              placeholder="Задайте вопрос об архивных записях..."
              class="flex-1 px-3 py-2.5 text-sm bg-white border border-border focus:border-ink focus:outline-none transition-colors"
              :disabled="isStreaming"
            />
            <button
              v-if="isStreaming"
              type="button"
              class="px-4 py-2.5 bg-accent text-white text-sm font-medium hover:bg-accent-hover transition-colors"
              aria-label="Остановить генерацию"
              @click="abort"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="6" y="6" width="12" height="12" rx="2" />
              </svg>
            </button>
            <button
              v-else
              type="submit"
              class="px-4 py-2.5 bg-ink text-white text-sm font-medium hover:bg-ink/90 transition-colors"
              aria-label="Отправить сообщение"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M5 12h14" />
                <path d="m12 5 7 7-7 7" />
              </svg>
            </button>
          </form>
          <p class="mt-2 text-[11px] text-ink-muted">
            Ответы генерируются на основе архивных документов. Проверяйте информацию.
          </p>
        </footer>
      </aside>
    </Transition>
  </Teleport>
</template>
