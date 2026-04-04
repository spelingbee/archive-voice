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

const { t } = useI18n()

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const {
  messages,
  currentResponse,
  sessionId,
  isStreaming,
  isConnecting,
  sendMessage,
  status: chatStatus,
  reset,
  error: streamError,
  lastSources,
  sessions,
  fetchSessions,
  loadSession,
} = useChatStream()

// --- Локальный стейт ---
const inputMessage = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
/** Текущий вид: 'chat' (активный диалог) или 'history' (список сессий) */
const currentView = ref<'chat' | 'history'>('chat')

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

  inputMessage.value = ''
  scrollToBottom()

  // Запускаем отправку (логика добавления в messages теперь внутри sendMessage)
  await sendMessage(prompt)
}

/** Смена вида */
function toggleView(view: 'chat' | 'history') {
  currentView.value = view
  if (view === 'history') {
    fetchSessions()
  }
}

/** Загрузка сессии из истории */
async function selectSession(id: string) {
  await loadSession(id)
  currentView.value = 'chat'
  scrollToBottom()
}

/** Начало нового чата */
function startNewChat() {
  reset()
  currentView.value = 'chat'
}

/** Форматирование даты для списка сессий */
const { locale } = useI18n()
function formatSessionDate(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleDateString(locale.value === 'ru' ? 'ru-RU' : 'en-US', {
      day: 'numeric',
      month: 'short',
    })
  } catch {
    return dateStr
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
        <header class="flex items-center justify-between px-5 py-4 border-b border-border bg-paper-dark/30">
          <div>
            <h2
              id="chat-title"
              class="text-lg font-semibold text-ink"
              style="font-family: var(--font-serif)"
            >
              {{ t('chat.global_title') }}
            </h2>
            <div class="flex items-center gap-2 mt-0.5">
              <span v-if="sessionId" class="text-[10px] uppercase tracking-widest text-ink-muted">Session #{{ sessionId }}</span>
              <span v-else class="text-[10px] uppercase tracking-widest text-accent font-bold">{{ t('chat.new_session') }}</span>
            </div>
          </div>
          <div class="flex items-center gap-1">
            <!-- Вкладки Чат / История -->
            <button 
              class="p-2 transition-colors"
              :class="currentView === 'chat' ? 'text-ink bg-white shadow-sm border border-border' : 'text-ink-muted hover:text-ink'"
              @click="toggleView('chat')"
              :title="t('chat.view_chat')"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 21 1.9-5.7a8.5 8.5 0 1 1 3.8 3.8z"/></svg>
            </button>
            <button 
              class="p-2 transition-colors mr-2"
              :class="currentView === 'history' ? 'text-ink bg-white shadow-sm border border-border' : 'text-ink-muted hover:text-ink'"
              @click="toggleView('history')"
              :title="t('chat.view_history')"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 8V12L15 15"/><circle cx="12" cy="12" r="9"/></svg>
            </button>
            
            <button
              type="button"
              class="p-2 text-ink-muted hover:text-ink transition-colors"
              :aria-label="t('common.close')"
              @click="handleClose"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 6 6 18" />
                <path d="m6 6 12 12" />
              </svg>
            </button>
          </div>
        </header>

        <!-- Main Content (Chat or History) -->
        <div class="flex-1 overflow-hidden flex flex-col">
          
          <!-- View: Chat -->
          <template v-if="currentView === 'chat'">
            <div ref="messagesContainer" class="flex-1 overflow-y-auto">
              <div class="divide-y divide-border">
                <article
                  v-for="message in messages"
                  :key="message.id"
                  class="px-5 py-4 transition-colors hover:bg-paper-dark/10"
                >
                  <header class="mb-2 flex justify-between items-center">
                    <h3
                      class="text-[10px] uppercase tracking-widest font-bold"
                      :class="message.role === 'user' ? 'text-ink-muted' : 'text-accent'"
                    >
                      {{ message.role === 'user' ? t('chat.user_label') : t('chat.system_label') }}
                    </h3>
                  </header>
                  <p class="text-sm text-ink leading-relaxed whitespace-pre-wrap">
                    {{ message.content }}
                  </p>
                  <footer v-if="message.source" class="mt-3">
                    <span
                      class="inline-flex items-center px-2 py-1 text-[10px] text-ink-muted bg-paper-dark border border-border"
                      style="font-family: var(--font-mono)"
                    >
                      {{ t('chat.source_label') }}: {{ message.source }}
                    </span>
                  </footer>
                </article>

                <!-- Текущий стриминг ответа (typewriter) -->
                <article
                  v-if="isStreaming || (isConnecting && !currentResponse)"
                  class="px-5 py-4 bg-paper-dark/20"
                >
                  <header class="mb-2">
                    <h3 class="text-[10px] uppercase tracking-widest font-bold text-accent">
                      {{ t('chat.system_label') }}
                    </h3>
                  </header>
                  <p v-if="isConnecting && !currentResponse" class="text-sm text-ink-muted animate-pulse">
                    {{ t('chat.searching') }}
                  </p>
                  <p v-else class="text-sm text-ink leading-relaxed whitespace-pre-wrap">
                    {{ currentResponse }}<span class="animate-pulse">▍</span>
                  </p>
                </article>
              </div>

              <!-- Пустое состояние -->
              <div
                v-if="messages.length === 0 && !isStreaming && !isConnecting"
                class="flex flex-col items-center justify-center h-full text-center px-8 py-16"
              >
                <div class="w-16 h-16 bg-paper-dark rounded-full flex items-center justify-center mb-4 text-ink-muted">
                  <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="m3 21 1.9-5.7a8.5 8.5 0 1 1 3.8 3.8z"/></svg>
                </div>
                <p class="text-sm text-ink-muted leading-relaxed">
                  {{ t('chat.global_empty') }}
                </p>
              </div>
            </div>

            <!-- Input Area -->
            <footer class="border-t border-border p-4 bg-white">
              <form class="flex gap-3" @submit.prevent="handleSubmit">
                <input
                  v-model="inputMessage"
                  type="text"
                  :placeholder="t('chat.global_placeholder')"
                  class="flex-1 px-3 py-2.5 text-sm bg-white border border-border focus:border-ink focus:outline-none transition-colors"
                  :disabled="isStreaming"
                />
                <button
                  type="submit"
                  class="px-4 py-2.5 bg-ink text-white text-sm font-medium hover:bg-ink/90 transition-colors disabled:opacity-50"
                  :disabled="isStreaming || isConnecting || !inputMessage.trim()"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M5 12h14" /><path d="m12 5 7 7-7 7" />
                  </svg>
                </button>
              </form>
              <div v-if="streamError" class="mt-2 text-xs text-accent">
                {{ streamError }}
              </div>
            </footer>
          </template>

          <!-- View: History -->
          <template v-else>
            <div class="flex-1 overflow-y-auto p-5">
              <header class="flex items-center justify-between mb-6">
                <h3 class="text-xs uppercase tracking-widest font-bold text-ink-muted">{{ t('chat.previous_sessions') }}</h3>
                <button 
                  class="text-xs font-bold text-accent flex items-center gap-1 hover:underline"
                  @click="startNewChat"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14"/></svg>
                  {{ t('chat.new_chat') }}
                </button>
              </header>

              <div v-if="sessions.length" class="space-y-3">
                <button
                  v-for="session in sessions"
                  :key="session.id"
                  class="w-full p-4 text-left border transition-all hover:shadow-md group flex items-start gap-4"
                  :class="sessionId === session.id ? 'border-accent bg-paper-dark/30' : 'border-border hover:border-ink/20'"
                  @click="selectSession(session.id)"
                >
                  <div class="flex-1 min-w-0">
                    <p class="text-sm text-ink font-medium truncate mb-1" :title="session.firstQuestion">
                      {{ session.firstQuestion }}
                    </p>
                    <div class="flex items-center gap-3 text-[10px] text-ink-muted uppercase">
                      <span>{{ formatSessionDate(session.date) }}</span>
                      <span class="flex items-center gap-1">
                        <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                        {{ session.messagesCount }}
                      </span>
                    </div>
                  </div>
                  <div v-if="sessionId === session.id" class="text-accent">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                  </div>
                </button>
              </div>
              <div v-else-if="chatStatus === 'connecting'" class="text-center py-12">
                <svg class="animate-spin h-6 w-6 mx-auto text-ink-muted mb-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                <p class="text-xs text-ink-muted">{{ t('common.loading') }}...</p>
              </div>
              <div v-else class="text-center py-12 text-sm text-ink-muted border-2 border-dashed border-paper-dark">
                {{ t('chat.no_history') }}
              </div>
            </div>
          </template>

        </div>
      </aside>
    </Transition>
  </Teleport>
</template>
