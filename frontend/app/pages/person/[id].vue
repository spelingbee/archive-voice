<script setup lang="ts">
/**
 * pages/person/[id].vue
 * Личная страница («Досье») репрессированного.
 *
 * Особенности:
 * — Динамическая загрузка через archiveRepository.getById()
 * — Интеграция контекстного ИИ-чата (RAG)
 * — Документальный, «архивный» стиль оформления
 */
import { archiveRepository } from '~/features/archive/api'

const route = useRoute()
const personId = computed(() => Number(route.params.id))

// --- Загрузка данных ---
const { data: person, status, error } = archiveRepository.getById(personId.value)

const isLoading = computed(() => status.value === 'pending')

/** Статус верификации */
const statusLabel = computed(() =>
  person.value?.status === 'verified' ? 'Верифицировано' : 'На проверке',
)

// --- SEO ---
useHead({
  title: computed(() => person.value ? `${person.value.fullName} — Голос из архива` : 'Запись архива'),
})
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <!-- Кнопка назад -->
    <nav class="mb-8">
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
      <p class="text-ink-muted animate-pulse">Загрузка материалов дела...</p>
    </div>

    <div v-else-if="error" class="py-24 text-center">
      <p class="text-accent mb-4">Дело не найдено или произошла ошибка.</p>
      <NuxtLink to="/" class="text-ink underline">Вернуться на главную</NuxtLink>
    </div>

    <!-- Основной контент (Досье) -->
    <div v-else-if="person" class="grid grid-cols-1 lg:grid-cols-3 gap-10">
      <!-- Личное дело (Слева) -->
      <article class="lg:col-span-2 space-y-10">
        <!-- Шапка досье -->
        <header class="border-b border-border pb-8">
          <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
            <div>
              <p class="text-[10px] uppercase tracking-[0.2em] text-ink-muted mb-2 font-mono">
                Архивное дело №{{ person.id }}
              </p>
              <h1
                class="text-3xl md:text-5xl font-bold text-ink leading-tight"
                style="font-family: var(--font-serif)"
              >
                {{ person.fullName }}
              </h1>
            </div>

            <!-- Бейдж статуса -->
            <div class="flex items-center gap-2">
              <span
                class="inline-flex items-center gap-1.5 px-3 py-1 text-xs font-medium border"
                :class="person.status === 'verified' ? 'text-verified border-verified bg-verified-bg' : 'text-ink-muted border-border bg-paper-dark'"
              >
                <span v-if="person.status === 'verified'" class="w-2 h-2 bg-verified rounded-full" />
                {{ statusLabel }}
              </span>
            </div>
          </div>
        </header>

        <!-- Анкетные данные в стиле картотеки -->
        <section>
          <h2 class="text-xs uppercase tracking-widest text-ink-muted mb-6 font-medium">
            Анкетные данные
          </h2>
          <dl class="grid grid-cols-1 md:grid-cols-2 gap-x-12 gap-y-6 border-t border-border pt-6">
            <div>
              <dt class="text-xs text-ink-muted mb-1 uppercase tracking-wider">Годы жизни</dt>
              <dd class="text-lg text-ink font-mono">
                {{ person.birthYear }} — {{ person.deathYear || '?' }}
              </dd>
            </div>
            <div>
              <dt class="text-xs text-ink-muted mb-1 uppercase tracking-wider">Регион</dt>
              <dd class="text-lg text-ink">{{ person.region }}</dd>
            </div>
            <div class="md:col-span-2">
              <dt class="text-xs text-ink-muted mb-1 uppercase tracking-wider">Обвинение</dt>
              <dd class="text-lg text-ink leading-relaxed">
                {{ person.accusation }}
              </dd>
            </div>
          </dl>
        </section>

        <!-- Биография / Обстоятельства (TODO: добавить в модель если нужно больше полей) -->
        <section v-if="person.biography || person.accusation">
          <h2 class="text-xs uppercase tracking-widest text-ink-muted mb-6 font-medium">
            Обстоятельства дела
          </h2>
          <div class="prose prose-sm max-w-none text-ink leading-relaxed">
            <p v-if="person.biography" class="whitespace-pre-wrap">{{ person.biography }}</p>
            <p v-else class="text-ink-muted italic">Подробные биографические сведения в процессе оцифровки.</p>
          </div>
        </section>

        <!-- Список документов (MOCK для демонстрации UI) -->
        <section>
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-xs uppercase tracking-widest text-ink-muted font-medium">
              Материалы дела
            </h2>
            <span class="text-[10px] text-ink-muted uppercase tracking-widest">
              Оцифровано: 4 документа
            </span>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div
              v-for="i in 4"
              :key="i"
              class="group flex items-center gap-4 p-4 border border-border hover:border-border-hover transition-colors bg-white cursor-pointer"
            >
               <div class="p-2 bg-paper-dark text-ink-muted group-hover:text-ink transition-colors">
                 <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                   <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
                   <polyline points="14 2 14 8 20 8" />
                 </svg>
               </div>
               <div>
                 <p class="text-sm font-medium text-ink">Документ #{{ i }}</p>
                 <p class="text-[10px] text-ink-muted uppercase">PDF · 1.2 MB</p>
               </div>
            </div>
          </div>
        </section>
      </article>

      <!-- Правая колонка: ИИ-ассистент (Контекстный чат) -->
      <aside>
        <div class="lg:sticky lg:top-24 space-y-6">
          <!-- Контекстный чат -->
          <ContextChat
            :person-id="person.id"
            title="Архивный ИИ-поиск"
            subtitle="Задайте вопрос об этом деле"
            class="h-[600px] shadow-sm"
          />

          <!-- Памятка -->
          <div class="p-5 border border-border bg-paper-dark/30">
            <h4 class="text-xs font-bold text-ink uppercase tracking-wider mb-2">Памятка исследователя</h4>
            <ul class="space-y-2">
              <li class="text-[11px] text-ink-secondary leading-relaxed flex gap-2">
                <span class="text-ink-muted opacity-50">•</span>
                Используйте ассистента для поиска связей между именами и датами в документах.
              </li>
              <li class="text-[11px] text-ink-secondary leading-relaxed flex gap-2">
                <span class="text-ink-muted opacity-50">•</span>
                Все ответы генерируются на основе загруженных материалов данного дела.
              </li>
            </ul>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>
