<script setup lang="ts">
/**
 * pages/person/[id].vue
 * Личная страница («Досье») репрессированного.
 * Полноценное отображение всех 13 архивных полей.
 *
 * Исправления:
 * — Статус: если запись публична → «Верифицировано» (не «На модерации»)
 * — Хронология: сворачивается через <details>
 * — ContextChat: ограниченная высота с внутренним скроллом
 * — Материалы дела: placeholder «В разработке»
 */
import { archiveRepository } from '~/features/archive/api'
import type { ModerationPerson } from '~/features/archive/types'

const route = useRoute()
const personId = computed(() => Number(route.params.id))

// --- Загрузка данных (кастим к ModerationPerson для доступа ко всем полям) ---
const { data: person, status, error } = archiveRepository.getById(personId as any) as any as { data: Ref<ModerationPerson>, status: Ref<string>, error: Ref<any> }

const isLoading = computed(() => status.value === 'pending')

// --- Сворачивание хронологии ---
const isChronologyOpen = ref(true)

/** Вспомогательная функция для форматирования дат */
function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '—'
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

// --- SEO ---
useHead({
  title: computed(() => person.value ? `${person.value.fullName} — Голос из архива` : 'Запись архива'),
})
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 md:py-8">
    <!-- Кнопка назад -->
    <nav class="mb-6 md:mb-8">
      <NuxtLink
        to="/"
        class="inline-flex items-center gap-2 text-ink-secondary hover:text-ink transition-colors group"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="group-hover:-translate-x-0.5 transition-transform">
          <path d="m15 18-6-6 6-6" />
        </svg>
        <span class="text-sm font-medium">Вернуться к списку</span>
      </NuxtLink>
    </nav>

    <!-- Ошибки / Загрузка -->
    <div v-if="isLoading" class="py-24 text-center">
      <p class="text-ink-muted animate-pulse font-serif italic text-lg">Извлечение материалов дела №{{ personId }}...</p>
    </div>

    <div v-else-if="error" class="py-24 text-center border border-dashed border-accent/30 bg-paper-dark">
      <p class="text-accent mb-4 font-medium">Запись временно недоступна или не найдена в основном реестре.</p>
      <NuxtLink to="/" class="text-ink underline decoration-dotted underline-offset-4">Вернуться в главный архив</NuxtLink>
    </div>

    <!-- Основной контент (Досье) -->
    <div v-else-if="person" class="grid grid-cols-1 lg:grid-cols-3 gap-8 lg:gap-12">
      
      <!-- Личное дело (Слева) -->
      <article class="lg:col-span-2 space-y-8 md:space-y-12">
        <!-- Шапка досье -->
        <header class="border-b border-border pb-6 md:pb-10">
          <div class="flex flex-col gap-4">
            <div class="space-y-3">
              <div class="flex items-center gap-3 flex-wrap">
                <span class="text-[10px] uppercase tracking-[0.3em] px-2 py-1 bg-accent text-white">Архив</span>
                <p class="text-[10px] uppercase tracking-[0.2em] text-ink-muted font-mono">
                  Дело №{{ person.id }}
                </p>
                <!-- Бейдж статуса: если запись публична — она верифицирована -->
                <span class="inline-flex items-center gap-1.5 px-3 py-1 text-[10px] font-bold uppercase tracking-widest border text-verified border-verified bg-verified-bg/30">
                  <span class="w-1.5 h-1.5 bg-verified rounded-full" />
                  Верифицировано
                </span>
              </div>
              <h1
                class="text-3xl md:text-5xl lg:text-6xl font-bold text-ink leading-tight"
                style="font-family: var(--font-serif)"
              >
                {{ person.fullName }}
              </h1>
            </div>
          </div>
        </header>

        <!-- Биография -->
        <section>
          <h2 class="text-[10px] uppercase tracking-[0.2em] text-ink-muted mb-6 md:mb-8 font-bold border-b border-border pb-1">
            I. Биографическая справка
          </h2>
          <div class="prose prose-sm max-w-none text-ink leading-relaxed">
            <p v-if="person.biography" class="whitespace-pre-wrap text-base md:text-lg leading-loose font-serif italic text-ink-secondary">
              "{{ person.biography }}"
            </p>
            <p v-else class="text-ink-muted italic opacity-60">
              Подробные биографические сведения находятся в процессе оцифровки из бумажных носителей.
            </p>
          </div>
        </section>

        <!-- Сведения о деле -->
        <section>
          <h2 class="text-[10px] uppercase tracking-[0.2em] text-ink-muted mb-6 md:mb-8 font-bold border-b border-border pb-1">
            II. Сведения о деле
          </h2>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-8 md:gap-x-12 gap-y-6 md:gap-y-8 bg-paper-dark/20 p-4 md:p-6 border border-border">
            <div>
              <dt class="text-[10px] text-ink-muted mb-1 uppercase tracking-widest">Статья обвинения</dt>
              <dd class="text-sm md:text-base text-ink font-medium">{{ person.charge || person.accusation || '—' }}</dd>
            </div>
            <div>
              <dt class="text-[10px] text-ink-muted mb-1 uppercase tracking-widest">Архивный источник</dt>
              <dd class="text-sm md:text-base text-ink font-serif italic">{{ person.source || 'ИЦ ГКНБ' }}</dd>
            </div>
            <div>
              <dt class="text-[10px] text-ink-muted mb-1 uppercase tracking-widest">Место жительства</dt>
              <dd class="text-sm md:text-base text-ink">{{ person.region }}{{ person.district ? `, ${person.district}` : '' }}</dd>
            </div>
            <div>
              <dt class="text-[10px] text-ink-muted mb-1 uppercase tracking-widest">Род занятий</dt>
              <dd class="text-sm md:text-base text-ink">{{ person.occupation || 'Сведения отсутствуют' }}</dd>
            </div>
            <div class="sm:col-span-2">
              <dt class="text-[10px] text-ink-muted mb-1 uppercase tracking-widest">Приговор (Мера наказания)</dt>
              <dd class="text-sm md:text-base text-ink font-bold">{{ person.sentence || 'Сведения о приговоре отсутствуют' }}</dd>
            </div>
          </div>
        </section>

        <!-- Материалы дела (Реальные документы) -->
        <section>
          <div class="flex items-center justify-between mb-6 border-b border-border pb-1">
            <h2 class="text-[10px] uppercase tracking-[0.2em] text-ink-muted font-bold">
              III. Материалы дела
            </h2>
            <span v-if="person.documents?.length" class="text-[10px] text-ink-muted uppercase tracking-widest">
              {{ person.documents.length }} документ{{ person.documents.length > 1 ? 'а/ов' : '' }}
            </span>
          </div>

          <!-- Список реальных документов -->
          <div v-if="person.documents?.length" class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div
              v-for="doc in person.documents"
              :key="doc.id"
              class="group flex items-center gap-3 p-4 border border-border hover:border-border-hover transition-all bg-white cursor-pointer"
              @click="archiveRepository.downloadDocument(doc.id, doc.originalFileName)"
            >
              <div class="p-2.5 bg-paper-dark text-ink-muted group-hover:text-accent transition-colors shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
                  <polyline points="14 2 14 8 20 8" />
                </svg>
              </div>
              <div class="min-w-0 flex-1">
                <p class="text-sm font-medium text-ink truncate group-hover:text-accent transition-colors">
                  {{ doc.originalFileName }}
                </p>
                <p class="text-[10px] text-ink-muted uppercase tracking-widest">
                  {{ doc.fileType }} · {{ (doc.fileSize / 1024).toFixed(0) }} KB
                </p>
              </div>
              <div class="shrink-0 text-ink-muted group-hover:text-accent transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="7 10 12 15 17 10" />
                  <line x1="12" y1="15" x2="12" y2="3" />
                </svg>
              </div>
            </div>
          </div>

          <!-- Нет документов -->
          <div v-else class="p-6 border border-dashed border-border text-center">
            <p class="text-sm text-ink-muted">Документы к этому делу пока не прикреплены.</p>
          </div>
        </section>
      </article>

      <!-- Правая колонка: Хронология и ИИ -->
      <aside class="space-y-6 md:space-y-8">
        <div class="lg:sticky lg:top-16 space-y-6 md:space-y-8 lg:max-h-[calc(100vh-5rem)] lg:overflow-y-auto">
          <!-- Блок хронологии (Сворачиваемый) -->
          <div class="bg-paper-dark border border-border">
            <button
              class="w-full flex items-center justify-between px-4 md:px-6 py-3 md:py-4 text-left"
              @click="isChronologyOpen = !isChronologyOpen"
            >
              <h3 class="text-[10px] uppercase tracking-[0.2em] text-ink-muted font-bold">
                Хронология дела
              </h3>
              <svg
                xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24"
                fill="none" stroke="currentColor" stroke-width="2"
                class="text-ink-muted transition-transform"
                :class="isChronologyOpen ? 'rotate-180' : ''"
              >
                <path d="m6 9 6 6 6-6" />
              </svg>
            </button>
            
            <div v-show="isChronologyOpen" class="px-4 md:px-6 pb-4 md:pb-6 space-y-5">
              <ul class="space-y-5">
                <li class="relative pl-5 border-l border-border pb-1">
                  <span class="absolute left-[-4px] top-0 w-2 h-2 bg-border rounded-full" />
                  <p class="text-[10px] text-ink-muted uppercase mb-0.5">Год рождения</p>
                  <p class="text-sm font-serif font-bold">{{ person.birthYear }}</p>
                </li>
                <li class="relative pl-5 border-l border-border pb-1">
                  <span class="absolute left-[-4px] top-0 w-2 h-2 bg-border rounded-full" />
                  <p class="text-[10px] text-ink-muted uppercase mb-0.5">Дата ареста</p>
                  <p class="text-sm font-bold">{{ formatDate(person.arrestDate) }}</p>
                </li>
                <li class="relative pl-5 border-l border-border pb-1">
                  <span class="absolute left-[-4px] top-0 w-2 h-2 bg-border rounded-full" />
                  <p class="text-[10px] text-ink-muted uppercase mb-0.5">Дата приговора</p>
                  <p class="text-sm font-bold">{{ formatDate(person.sentenceDate) }}</p>
                </li>
                <li v-if="person.deathYear" class="relative pl-5 border-l border-border pb-1">
                  <span class="absolute left-[-4px] top-0 w-2 h-2 bg-ink rounded-full" />
                  <p class="text-[10px] text-ink-muted uppercase mb-0.5">Год смерти</p>
                  <p class="text-sm font-serif font-bold text-ink">{{ person.deathYear }}</p>
                </li>
                <li class="relative pl-5 border-l border-ink/30">
                  <span class="absolute left-[-4px] top-0 w-2 h-2 bg-verified rounded-full ring-2 ring-verified-bg" />
                  <p class="text-[10px] text-verified font-bold uppercase mb-0.5 tracking-widest">Реабилитация</p>
                  <p class="text-sm md:text-base font-bold text-verified">{{ formatDate(person.rehabilitationDate) }}</p>
                </li>
              </ul>
            </div>
          </div>

          <!-- Контекстный чат (RAG) — ограниченная высота с внутренним скроллом -->
          <ContextChat
            :person-id="person.id"
            title="Архивный ИИ-поиск"
            subtitle="Задайте вопрос об этом деле"
            class="h-[350px] md:h-[400px] shadow-sm border border-border"
          />

          <!-- Техническая справка -->
          <div class="p-4 md:p-6 border border-border bg-ink text-white/90">
            <h4 class="text-[10px] font-bold uppercase tracking-widest mb-2">Техническая справка</h4>
            <p class="text-[10px] md:text-[11px] leading-relaxed opacity-80">
              Информация из архивных фондов. Проект КТМУ Манас.
              Если вы нашли ошибку, воспользуйтесь обратной связью.
            </p>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>
