<script setup lang="ts">
/**
 * pages/dashboard.vue
 * Личный кабинет пользователя.
 */
import type { ChatSession } from '~/features/chat/types'
import { archiveRepository } from '~/features/archive/api'
import type { ModerationPerson } from '~/features/archive/types'

// --- SEO ---
useHead({ title: 'Личный кабинет — Голос из архива' })

// --- Auth ---
const { user, isAuthenticated, isModerator, isAdmin, fullName, logout } = useAuth()

// --- Moderation Data ---
const { data: moderationData, refresh: refreshModeration } = await archiveRepository.getModerationList()
const moderationList = computed(() => moderationData.value?.content || [])
const pendingCount = computed(() => moderationData.value?.totalElements || 0)

// --- User Records Data ---
const { data: userData, refresh: refreshUserRecords } = await archiveRepository.getMyList()
const userRecords = computed(() => userData.value?.content || [])
const userRecordsCount = computed(() => userData.value?.totalElements || 0)

// --- Moderation Actions ---
const selectedModerationItem = ref<ModerationPerson | null>(null)
const isModerationDetailOpen = ref(false)
const isRejecting = ref(false)
const rejectionReason = ref('')
const isActionLoading = ref(false)

// --- Notifications ---
interface Notification {
  show: boolean
  message: string
  type: 'success' | 'error'
}
const notification = ref<Notification>({ show: false, message: '', type: 'success' })
let notificationTimeout: any = null

function showNotification(message: string, type: 'success' | 'error' = 'success') {
  if (notificationTimeout) clearTimeout(notificationTimeout)
  notification.value = { show: true, message, type }
  notificationTimeout = setTimeout(() => {
    notification.value.show = false
  }, 5000)
}

function openModerationDetail(item: ModerationPerson) {
  selectedModerationItem.value = item
  isModerationDetailOpen.value = true
}

function closeModerationDetail() {
  isModerationDetailOpen.value = false
  selectedModerationItem.value = null
  isRejecting.value = false
  rejectionReason.value = ''
}

async function handleApprove() {
  if (!selectedModerationItem.value) return
  isActionLoading.value = true
  try {
    await archiveRepository.approve(selectedModerationItem.value.id)
    showNotification('Запись успешно одобрена и опубликована', 'success')
    await refreshModeration()
    closeModerationDetail()
  } catch (err: any) {
    showNotification(err.response?._data?.message || 'Ошибка при одобрении', 'error')
  } finally {
    isActionLoading.value = false
  }
}

async function handleRejectConfirm() {
  if (!selectedModerationItem.value || !rejectionReason.value.trim()) return
  isActionLoading.value = true
  try {
    await archiveRepository.reject(selectedModerationItem.value.id, rejectionReason.value)
    showNotification('Запись была отклонена', 'success')
    await refreshModeration()
    closeModerationDetail()
  } catch (err: any) {
    showNotification(err.response?._data?.message || 'Ошибка при отклонении', 'error')
  } finally {
    isActionLoading.value = false
  }
}

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
    { key: 'moderation', label: 'Заявки на модерацию', badge: pendingCount.value, moderatorOnly: true },
    { key: 'ai-history', label: 'История запросов к ИИ' },
    { key: 'settings', label: 'Настройки' },
  ]
  return items.filter(item => !item.moderatorOnly || isModerator.value)
})

// --- AI Chat History (Mock) ---
const chatSessions = ref<ChatSession[]>([
  { id: 'sess-001', date: '2024-01-15', firstQuestion: 'Какие документы сохранились по делу Байтемирова Асана Жумабековича?', messagesCount: 8 },
  { id: 'sess-002', date: '2024-01-14', firstQuestion: 'Сколько человек было репрессировано в Чуйской области в 1937 году?', messagesCount: 5 },
  { id: 'sess-003', date: '2024-01-12', firstQuestion: 'Найди информацию о репрессированных учителях из Ошской области', messagesCount: 12 },
])

// --- Helpers ---
function formatDate(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleDateString('ru-RU', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    })
  } catch {
    return dateStr
  }
}

function truncateText(text: string, maxLength: number = 80): string {
  return text.length <= maxLength ? text : `${text.slice(0, maxLength).trim()}...`
}

// --- AI Dialog state ---
const selectedSession = ref<ChatSession | null>(null)
const isDialogOpen = ref(false)

function openSessionDetail(session: ChatSession) {
  selectedSession.value = session
  isDialogOpen.value = true
}

function closeDialog() {
  isDialogOpen.value = false
  selectedSession.value = null
}

const roleLabel = computed(() => {
  if (isAdmin.value) return 'Админ'
  if (isModerator.value) return 'Модератор'
  return null
})
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 md:px-8 py-8">
    <!-- Global Notifications -->
    <Transition enter-active-class="duration-300 ease-out" enter-from-class="transform -translate-y-4 opacity-0" enter-to-class="transform translate-y-0 opacity-100" leave-active-class="duration-200 ease-in" leave-from-class="opacity-100" leave-to-class="opacity-0">
      <div v-if="notification.show" class="fixed top-24 left-1/2 -translate-x-1/2 z-[100] w-full max-w-md px-4">
        <div 
          class="p-4 shadow-xl border flex items-center gap-3"
          :class="notification.type === 'success' ? 'bg-verified-bg border-verified text-verified' : 'bg-accent/10 border-accent text-accent'"
        >
          <svg v-if="notification.type === 'success'" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          <span class="text-sm font-medium">{{ notification.message }}</span>
          <button class="ml-auto" @click="notification.show = false">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
          </button>
        </div>
      </div>
    </Transition>

    <div class="flex flex-col lg:flex-row gap-8">
      <!-- Sidebar Navigation -->
      <aside class="lg:w-64 shrink-0">
        <nav class="sticky top-24">
          <h2 class="text-xs uppercase tracking-wider text-ink-muted mb-4 px-3">Личный кабинет</h2>
          <div v-if="roleLabel" class="px-3 mb-4">
            <span class="text-[10px] uppercase tracking-wide px-2 py-0.5 bg-paper-dark text-ink-muted border border-border">
              {{ roleLabel }}
            </span>
          </div>
          <ul class="space-y-1">
            <li v-for="item in navItems" :key="item.key">
              <button
                class="w-full flex items-center justify-between px-3 py-2.5 text-sm text-left transition-colors border-l-2"
                :class="activeTab === item.key ? 'border-ink bg-paper-dark text-ink font-medium' : 'border-transparent text-ink-secondary hover:text-ink hover:bg-paper-dark'"
                @click="activeTab = item.key"
              >
                <span>{{ item.label }}</span>
                <span v-if="item.badge" class="px-1.5 py-0.5 text-[10px] font-medium bg-accent text-white">{{ item.badge }}</span>
              </button>
            </li>
          </ul>
          <div class="mt-8 pt-6 border-t border-border">
            <NuxtLink to="/" class="flex items-center gap-2 px-3 py-2 text-sm text-ink-muted hover:text-ink transition-colors">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 19-7-7 7-7" /><path d="M19 12H5" /></svg>
              Вернуться в архив
            </NuxtLink>
          </div>
        </nav>
      </aside>

      <!-- Content Area -->
      <main class="flex-1 min-w-0">
        <!-- My Records -->
        <section v-if="activeTab === 'my-records'">
          <header class="mb-6">
            <h2 class="text-2xl font-semibold text-ink mb-2" style="font-family: var(--font-serif)">Мои записи</h2>
            <p class="text-sm text-ink-secondary">Записи, добавленные вами в архив</p>
          </header>
          
          <div v-if="userRecords.length" class="space-y-4">
            <article
              v-for="item in userRecords"
              :key="item.id"
              class="border border-border bg-white overflow-hidden group cursor-pointer hover:border-ink/20 transition-all"
              @click="openModerationDetail(item)"
            >
              <div class="p-5">
                <div class="flex justify-between items-start mb-3">
                  <div>
                    <h3 class="font-medium text-ink transition-colors group-hover:text-accent">{{ item.fullName }}</h3>
                    <p class="text-xs text-ink-muted mt-1 uppercase tracking-wider font-mono">{{ item.birthYear }} г.р. • {{ item.region }}</p>
                  </div>
                  <div class="flex flex-col items-end gap-2">
                    <span 
                      class="px-2 py-1 text-[10px] font-semibold border uppercase"
                      :class="{
                        'bg-paper-dark border-border text-ink-muted': item.moderationStatus === 'PENDING',
                        'bg-verified-bg border-verified text-verified': item.moderationStatus === 'APPROVED',
                        'bg-accent/10 border-accent text-accent': item.moderationStatus === 'REJECTED'
                      }"
                    >
                      {{ item.moderationStatus }}
                    </span>
                  </div>
                </div>
                
                <p v-if="item.moderationStatus === 'REJECTED' && item.rejectionReason" class="text-xs text-accent mb-4 p-3 bg-accent/5 border border-accent/20 italic">
                  <strong>Причина отказа:</strong> {{ item.rejectionReason }}
                </p>
                
                <p class="text-sm text-ink-secondary mb-4 line-clamp-2 italic">"{{ item.biography || item.charge || 'Описание отсутствует' }}"</p>
                
                <div class="flex items-center justify-between pt-4 border-t border-border">
                  <div class="text-[10px] text-ink-muted uppercase tracking-wider">Добавлено: {{ formatDate(item.createdAt) }}</div>
                  <button class="px-4 py-2 text-xs font-medium text-ink border border-border hover:bg-paper-dark transition-colors">Подробнее</button>
                </div>
              </div>
            </article>
          </div>
          <div v-else class="border border-border bg-white p-12 text-center">
            <p class="text-ink-muted mb-4">У вас пока нет добавленных записей</p>
            <NuxtLink to="/add" class="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-ink hover:bg-ink/90 transition-colors">Добавить первую запись</NuxtLink>
          </div>
        </section>

        <!-- Moderation (Moderators Only) -->
        <section v-if="activeTab === 'moderation' && isModerator">
          <header class="mb-6">
            <h2 class="text-2xl font-semibold text-ink mb-2" style="font-family: var(--font-serif)">Заявки на модерацию</h2>
            <p class="text-sm text-ink-secondary">Записи, ожидающие проверки и подтверждения</p>
          </header>

          <div v-if="moderationList.length" class="space-y-4">
            <article
              v-for="item in moderationList"
              :key="item.id"
              class="border border-border bg-white overflow-hidden group cursor-pointer hover:border-ink/20 transition-all"
              @click="openModerationDetail(item)"
            >
              <div class="p-5">
                <div class="flex justify-between items-start mb-3">
                  <div>
                    <h3 class="font-medium text-ink group-hover:text-ink transition-colors">{{ item.fullName }}</h3>
                    <p class="text-xs text-ink-muted mt-1 uppercase tracking-wider font-mono">{{ item.birthYear }} г.р. • {{ item.region }}</p>
                  </div>
                  <span class="px-2 py-1 text-[10px] font-semibold bg-paper-dark border border-border text-ink-muted uppercase">{{ item.moderationStatus }}</span>
                </div>
                <p class="text-sm text-ink-secondary mb-4 line-clamp-2 italic">"{{ item.biography || item.charge || 'Описание отсутствует' }}"</p>
                <div class="flex items-center justify-between pt-4 border-t border-border">
                  <div class="text-[10px] text-ink-muted uppercase tracking-wider">От: {{ item.createdByEmail }}</div>
                  <button class="px-4 py-2 text-xs font-medium text-ink border border-border hover:bg-paper-dark transition-colors">Подробнее</button>
                </div>
              </div>
            </article>
          </div>
          <div v-else class="border border-border bg-white p-12 text-center">
            <p class="text-ink-muted">Нет новых заявок на модерацию</p>
          </div>
        </section>

        <!-- AI History -->
        <section v-if="activeTab === 'ai-history'">
          <header class="mb-6">
            <h2 class="text-2xl font-semibold text-ink mb-2" style="font-family: var(--font-serif)">История запросов к ИИ</h2>
            <p class="text-sm text-ink-secondary">Ваши предыдущие диалоги с ассистентом</p>
          </header>
          <div v-if="chatSessions.length" class="border border-border bg-white divide-y divide-border">
            <article v-for="session in chatSessions" :key="session.id" class="p-4 md:p-5 hover:bg-paper-dark transition-colors">
              <div class="flex flex-col md:flex-row md:items-center gap-3 md:gap-4">
                <time class="text-xs text-ink-muted font-mono md:w-32">{{ formatDate(session.date) }}</time>
                <div class="flex-1 min-w-0">
                  <p class="text-sm text-ink leading-snug">{{ truncateText(session.firstQuestion) }}</p>
                </div>
                <button class="px-3 py-1.5 text-xs font-medium border border-border hover:border-border-hover" @click="openSessionDetail(session)">Открыть диалог</button>
              </div>
            </article>
          </div>
        </section>

        <!-- Settings -->
        <section v-if="activeTab === 'settings'">
          <header class="mb-6">
            <h2 class="text-2xl font-semibold text-ink mb-2" style="font-family: var(--font-serif)">Настройки</h2>
          </header>
          <div class="border border-border bg-white divide-y divide-border p-5">
            <p class="text-sm"><strong>Email:</strong> {{ user?.email }}</p>
            <div class="mt-4 flex gap-4">
              <button class="px-3 py-1.5 text-xs font-medium text-ink-secondary border border-border" @click="logout">Выйти</button>
            </div>
          </div>
        </section>
      </main>
    </div>

    <!-- Teleport Modals -->
    <Teleport to="body">
      <!-- AI Chat Modal -->
      <Transition enter-active-class="duration-200 ease-out" enter-from-class="opacity-0" enter-to-class="opacity-100" leave-active-class="duration-150 ease-in" leave-from-class="opacity-100" leave-to-class="opacity-0">
        <div v-if="isDialogOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div class="absolute inset-0 bg-ink/50" @click="closeDialog" />
          <div v-if="selectedSession" class="relative bg-white border border-border w-full max-w-2xl max-h-[80vh] overflow-auto">
             <header class="sticky top-0 bg-white border-b border-border p-4 flex items-center justify-between">
                <h3 class="font-semibold">Диалог от {{ formatDate(selectedSession.date) }}</h3>
                <button @click="closeDialog">Закрыть</button>
             </header>
             <div class="p-5">
                <blockquote class="border-l-2 border-border pl-4 italic">{{ selectedSession.firstQuestion }}</blockquote>
                <div class="mt-4 text-center p-8 bg-paper-dark border border-border text-sm text-ink-muted">История сообщений будет загружена здесь...</div>
             </div>
          </div>
        </div>
      </Transition>

      <!-- Moderation Detail Modal -->
      <Transition enter-active-class="duration-200 ease-out" enter-from-class="opacity-0" enter-to-class="opacity-100" leave-active-class="duration-150 ease-in" leave-from-class="opacity-100" leave-to-class="opacity-0">
        <div v-if="isModerationDetailOpen" class="fixed inset-0 z-50 flex items-center justify-center p-0 md:p-4">
          <div v-if="selectedModerationItem" class="absolute inset-0 bg-ink/70 backdrop-blur-sm" @click="closeModerationDetail" />
          
          <div v-if="selectedModerationItem" class="relative bg-white border border-border w-full max-w-4xl h-full md:h-auto md:max-h-[90vh] overflow-hidden flex flex-col shadow-2xl">
            <header class="bg-paper-dark border-b border-border p-5 flex items-center justify-between">
              <div class="flex items-center gap-3">
                <h3 class="text-lg font-bold">{{ selectedModerationItem.fullName }}</h3>
              </div>
              <button @click="closeModerationDetail">Закрыть</button>
            </header>
            
            <div class="flex-1 overflow-auto p-6 grid grid-cols-1 md:grid-cols-3 gap-8">
              <div class="md:col-span-2 space-y-6">
                <div>
                  <h4 class="text-xs uppercase font-bold text-ink-muted mb-2">Биография</h4>
                  <p class="text-sm leading-relaxed">{{ selectedModerationItem.biography || 'Нет описания' }}</p>
                </div>
                <div class="grid grid-cols-2 gap-4 text-sm">
                  <div><strong>Статья:</strong> {{ selectedModerationItem.charge || selectedModerationItem.accusation }}</div>
                  <div><strong>Источник:</strong> {{ selectedModerationItem.source }}</div>
                  <div><strong>Приговор:</strong> {{ selectedModerationItem.sentence }}</div>
                  <div><strong>Район:</strong> {{ selectedModerationItem.district }}</div>
                </div>

                <!-- Attached Documents -->
                <div v-if="selectedModerationItem.documents?.length" class="mt-8 pt-6 border-t border-border">
                  <h4 class="text-xs uppercase font-bold text-ink-muted mb-4">Прикрепленные документы ({{ selectedModerationItem.documents.length }})</h4>
                  <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div 
                      v-for="doc in selectedModerationItem.documents" 
                      :key="doc.id"
                      class="flex items-center gap-3 p-3 border border-border bg-paper-dark hover:border-border-hover transition-colors group"
                    >
                      <div class="w-10 h-10 flex-shrink-0 bg-white border border-border flex items-center justify-center text-ink-muted">
                        <svg v-if="doc.fileType?.includes('pdf')" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>
                        <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg>
                      </div>
                      <div class="min-w-0 flex-1">
                        <p class="text-xs font-medium text-ink truncate" :title="doc.originalFileName">{{ doc.originalFileName }}</p>
                        <p class="text-[10px] text-ink-muted uppercase tracking-tight">{{ (doc.fileSize / 1024).toFixed(1) }} KB</p>
                      </div>
                      <button 
                        class="p-2 text-ink-muted hover:text-ink transition-colors"
                        title="Скачать"
                        @click="archiveRepository.downloadDocument(doc.id, doc.originalFileName)"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
              
              <div class="bg-paper-dark p-4 text-sm space-y-3">
                <div class="flex justify-between"><span>Рождение</span><span>{{ selectedModerationItem.birthYear }}</span></div>
                <div class="flex justify-between"><span>Арест</span><span>{{ selectedModerationItem.arrestDate || '—' }}</span></div>
                <div class="flex justify-between"><span>Приговор</span><span>{{ selectedModerationItem.sentenceDate || '—' }}</span></div>
                <div class="flex justify-between"><span>Реабилитация</span><span class="text-verified font-bold">{{ selectedModerationItem.rehabilitationDate || '—' }}</span></div>
                <div class="mt-4 pt-4 border-t border-border">
                   <strong>Автор:</strong> {{ selectedModerationItem.createdByEmail }}
                </div>
              </div>
            </div>

            <footer v-if="isModerator && selectedModerationItem.moderationStatus === 'PENDING'" class="bg-paper-dark border-t border-border p-5">
              <div v-if="isRejecting" class="space-y-4">
                <textarea v-model="rejectionReason" class="w-full p-3 border border-accent/30 text-sm" rows="3" placeholder="Укажите причину..."></textarea>
                <div class="flex justify-end gap-3 text-sm">
                   <button @click="isRejecting = false">Отмена</button>
                   <button 
                    :disabled="!rejectionReason.trim() || isActionLoading" 
                    class="px-4 py-2 bg-accent text-white hover:bg-accent-hover transition-colors flex items-center justify-center gap-2 min-w-[180px]" 
                    @click="handleRejectConfirm"
                   >
                    <svg v-if="isActionLoading" class="animate-spin" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>
                    <span>{{ isActionLoading ? 'Выполняется...' : 'Подтвердить отклонение' }}</span>
                   </button>
                </div>
              </div>
              <div v-else class="flex justify-between items-center">
                  <p class="text-[10px] text-ink-muted uppercase">Проверьте данные перед решением</p>
                  <div class="flex gap-4">
                    <button class="px-6 py-2 border border-border hover:bg-paper-dark transition-colors" @click="isRejecting = true">Отклонить</button>
                    <button 
                      :disabled="isActionLoading" 
                      class="px-8 py-2 bg-ink text-white hover:bg-ink/90 transition-colors flex items-center justify-center gap-2 min-w-[140px]" 
                      @click="handleApprove"
                    >
                      <svg v-if="isActionLoading" class="animate-spin" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>
                      <span>{{ isActionLoading ? 'Обработка...' : 'Одобрить' }}</span>
                    </button>
                  </div>
              </div>
            </footer>
            <footer v-else class="bg-paper-dark border-t border-border p-4 text-center">
              <span class="text-[10px] uppercase font-bold text-ink-muted tracking-widest">Просмотр архивной записи</span>
            </footer>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
