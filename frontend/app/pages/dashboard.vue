<script setup lang="ts">
/**
 * pages/dashboard.vue
 * Личный кабинет пользователя.
 *
 * Использует useAuth() вместо mock-данных.
 * Вкладки: Мои записи, Модерация (ADMIN/MODERATOR), История ИИ, Настройки.
 */
import type { ChatSession } from '~/features/chat/types'

// --- SEO ---
useHead({ title: 'Личный кабинет — Голос из архива' })

// --- Auth (вместо mock) ---
const { user, isAuthenticated, isModerator, isAdmin, fullName, logout } = useAuth()

// Редирект на /auth если не авторизован
watch(isAuthenticated, (val) => {
  if (!val) navigateTo('/auth')
}, { immediate: true })

// --- Tabs ---
type TabKey = 'my-records' | 'moderation' | 'ai-history' | 'settings'
const activeTab = ref<TabKey>('my-records')

interface NavItem {
  key: TabKey
  label: string
  badge?: number
  moderatorOnly?: boolean
}

const navItems = computed<NavItem[]>(() => {
  const items: NavItem[] = [
    { key: 'my-records', label: 'Мои записи' },
    { key: 'moderation', label: 'Заявки на модерацию', badge: 3, moderatorOnly: true },
    { key: 'ai-history', label: 'История запросов к ИИ' },
    { key: 'settings', label: 'Настройки' },
  ]
  return items.filter(item => !item.moderatorOnly || isModerator.value)
})

// --- AI Chat History (TODO: загрузка с API) ---
const chatSessions = ref<ChatSession[]>([
  {
    id: 'sess-001',
    date: '2024-01-15',
    firstQuestion: 'Какие документы сохранились по делу Байтемирова Асана Жумабековича?',
    messagesCount: 8,
  },
  {
    id: 'sess-002',
    date: '2024-01-14',
    firstQuestion: 'Сколько человек было репрессировано в Чуйской области в 1937 году?',
    messagesCount: 5,
  },
  {
    id: 'sess-003',
    date: '2024-01-12',
    firstQuestion: 'Найди информацию о репрессированных учителях из Ошской области',
    messagesCount: 12,
  },
  {
    id: 'sess-004',
    date: '2024-01-10',
    firstQuestion: 'Что такое статья 58-10 и за что по ней осуждали?',
    messagesCount: 6,
  },
  {
    id: 'sess-005',
    date: '2024-01-08',
    firstQuestion: 'Есть ли в архиве данные о реабилитации жертв репрессий после 1956 года?',
    messagesCount: 4,
  },
])

// --- Helpers ---
function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('ru-RU', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })
}

function truncateText(text: string, maxLength: number = 80): string {
  return text.length <= maxLength ? text : `${text.slice(0, maxLength).trim()}...`
}

// --- Dialog modal ---
const selectedSession = ref<ChatSession | null>(null)
const isDialogOpen = ref(false)

function openDialog(session: ChatSession) {
  selectedSession.value = session
  isDialogOpen.value = true
}

function closeDialog() {
  isDialogOpen.value = false
  selectedSession.value = null
}

/** Лейбл роли для отображения */
const roleLabel = computed(() => {
  if (isAdmin.value) return 'Админ'
  if (isModerator.value) return 'Модератор'
  return null
})
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 md:px-8 py-8">
    <div class="flex flex-col lg:flex-row gap-8">
      <!-- Sidebar Navigation -->
      <aside class="lg:w-64 shrink-0">
        <nav class="sticky top-24">
          <h2 class="text-xs uppercase tracking-wider text-ink-muted mb-4 px-3">
            Личный кабинет
          </h2>

          <!-- Бейдж роли -->
          <div v-if="roleLabel" class="px-3 mb-4">
            <span class="text-[10px] uppercase tracking-wide px-2 py-0.5 bg-paper-dark text-ink-muted border border-border">
              {{ roleLabel }}
            </span>
          </div>

          <ul class="space-y-1">
            <li v-for="item in navItems" :key="item.key">
              <button
                class="w-full flex items-center justify-between px-3 py-2.5 text-sm text-left transition-colors border-l-2"
                :class="
                  activeTab === item.key
                    ? 'border-ink bg-paper-dark text-ink font-medium'
                    : 'border-transparent text-ink-secondary hover:text-ink hover:bg-paper-dark'
                "
                @click="activeTab = item.key"
              >
                <span>{{ item.label }}</span>
                <span
                  v-if="item.badge"
                  class="px-1.5 py-0.5 text-[10px] font-medium bg-accent text-white"
                >
                  {{ item.badge }}
                </span>
              </button>
            </li>
          </ul>

          <div class="mt-8 pt-6 border-t border-border">
            <NuxtLink
              to="/"
              class="flex items-center gap-2 px-3 py-2 text-sm text-ink-muted hover:text-ink transition-colors"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="m12 19-7-7 7-7" />
                <path d="M19 12H5" />
              </svg>
              Вернуться в архив
            </NuxtLink>
          </div>
        </nav>
      </aside>

      <!-- Content Area -->
      <main class="flex-1 min-w-0">
        <!-- My Records Tab -->
        <section v-if="activeTab === 'my-records'">
          <header class="mb-6">
            <h2 class="text-2xl font-semibold text-ink mb-2" style="font-family: var(--font-serif)">
              Мои записи
            </h2>
            <p class="text-sm text-ink-secondary">Записи, добавленные вами в архив</p>
          </header>
          <div class="border border-border bg-white p-8 text-center">
            <p class="text-ink-muted mb-4">У вас пока нет добавленных записей</p>
            <NuxtLink
              to="/add"
              class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-ink hover:bg-ink/90 transition-colors"
            >
              Добавить первую запись
            </NuxtLink>
          </div>
        </section>

        <!-- Moderation Tab (Moderators Only) -->
        <section v-if="activeTab === 'moderation' && isModerator">
          <header class="mb-6">
            <h2 class="text-2xl font-semibold text-ink mb-2" style="font-family: var(--font-serif)">
              Заявки на модерацию
            </h2>
            <p class="text-sm text-ink-secondary">Записи, ожидающие проверки и подтверждения</p>
          </header>
          <div class="border border-border bg-white p-8 text-center">
            <p class="text-ink-muted">Раздел модерации в разработке</p>
          </div>
        </section>

        <!-- AI History Tab -->
        <section v-if="activeTab === 'ai-history'">
          <header class="mb-6">
            <h2 class="text-2xl font-semibold text-ink mb-2" style="font-family: var(--font-serif)">
              История запросов к ИИ
            </h2>
            <p class="text-sm text-ink-secondary">Ваши предыдущие диалоги с архивным ассистентом</p>
          </header>

          <div v-if="chatSessions.length" class="border border-border bg-white divide-y divide-border">
            <article
              v-for="session in chatSessions"
              :key="session.id"
              class="p-4 md:p-5 hover:bg-paper-dark transition-colors"
            >
              <div class="flex flex-col md:flex-row md:items-center gap-3 md:gap-4">
                <time
                  :datetime="session.date"
                  class="text-xs text-ink-muted shrink-0 md:w-32"
                  style="font-family: var(--font-mono)"
                >
                  {{ formatDate(session.date) }}
                </time>
                <div class="flex-1 min-w-0">
                  <p class="text-sm text-ink leading-snug">
                    {{ truncateText(session.firstQuestion) }}
                  </p>
                  <p class="text-xs text-ink-muted mt-1">
                    {{ session.messagesCount }} сообщений в диалоге
                  </p>
                </div>
                <button
                  class="self-start md:self-center px-3 py-1.5 text-xs font-medium text-ink border border-border hover:border-border-hover transition-colors shrink-0"
                  @click="openDialog(session)"
                >
                  Открыть диалог
                </button>
              </div>
            </article>
          </div>

          <div v-else class="border border-border bg-white p-8 text-center">
            <p class="text-ink-muted">У вас пока нет сохраненных диалогов с ИИ</p>
          </div>
        </section>

        <!-- Settings Tab -->
        <section v-if="activeTab === 'settings'">
          <header class="mb-6">
            <h2 class="text-2xl font-semibold text-ink mb-2" style="font-family: var(--font-serif)">
              Настройки
            </h2>
            <p class="text-sm text-ink-secondary">Управление аккаунтом и предпочтениями</p>
          </header>

          <div class="border border-border bg-white divide-y divide-border">
            <div class="p-5">
              <h3 class="text-sm font-medium text-ink mb-4">Профиль</h3>
              <dl class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                <div>
                  <dt class="text-ink-muted mb-1">Имя</dt>
                  <dd class="text-ink">{{ user?.firstName }}</dd>
                </div>
                <div>
                  <dt class="text-ink-muted mb-1">Фамилия</dt>
                  <dd class="text-ink">{{ user?.lastName }}</dd>
                </div>
                <div>
                  <dt class="text-ink-muted mb-1">Email</dt>
                  <dd class="text-ink">{{ user?.email }}</dd>
                </div>
              </dl>
            </div>
            <div class="p-5">
              <h3 class="text-sm font-medium text-accent mb-4">Опасная зона</h3>
              <div class="flex gap-4">
                <button
                  class="px-3 py-1.5 text-xs font-medium text-accent border border-accent/30 hover:border-accent transition-colors"
                >
                  Удалить аккаунт
                </button>
                <button
                  class="px-3 py-1.5 text-xs font-medium text-ink-secondary border border-border hover:border-border-hover transition-colors"
                  @click="logout"
                >
                  Выйти из аккаунта
                </button>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>

    <!-- Dialog Modal -->
    <Teleport to="body">
      <Transition
        enter-active-class="duration-200 ease-out"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="duration-150 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div
          v-if="isDialogOpen"
          class="fixed inset-0 z-50 flex items-center justify-center p-4"
        >
          <div class="absolute inset-0 bg-ink/50" @click="closeDialog" />

          <div
            v-if="selectedSession"
            class="relative bg-white border border-border w-full max-w-2xl max-h-[80vh] overflow-auto"
          >
            <header class="sticky top-0 bg-white border-b border-border p-4 flex items-center justify-between">
              <div>
                <h3 class="text-lg font-semibold text-ink" style="font-family: var(--font-serif)">
                  Диалог от {{ formatDate(selectedSession.date) }}
                </h3>
                <p class="text-xs text-ink-muted mt-0.5">
                  ID сессии: {{ selectedSession.id }}
                </p>
              </div>
              <button class="p-2 text-ink-muted hover:text-ink transition-colors" @click="closeDialog">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M18 6 6 18" />
                  <path d="m6 6 12 12" />
                </svg>
              </button>
            </header>

            <div class="p-5">
              <p class="text-sm text-ink-secondary mb-4">Первый вопрос:</p>
              <blockquote class="text-ink border-l-2 border-border pl-4 mb-6">
                {{ selectedSession.firstQuestion }}
              </blockquote>
              <div class="bg-paper-dark border border-border p-4 text-center">
                <p class="text-sm text-ink-muted">
                  Полное содержимое диалога будет загружено из базы данных.
                </p>
                <p class="text-xs text-ink-muted mt-2">
                  {{ selectedSession.messagesCount }} сообщений в этом диалоге
                </p>
              </div>
            </div>

            <footer class="border-t border-border p-4 flex justify-end gap-3">
              <button
                class="px-4 py-2 text-sm text-ink-secondary hover:text-ink transition-colors"
                @click="closeDialog"
              >
                Закрыть
              </button>
              <button class="px-4 py-2 text-sm font-medium text-white bg-ink hover:bg-ink/90 transition-colors">
                Продолжить диалог
              </button>
            </footer>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
