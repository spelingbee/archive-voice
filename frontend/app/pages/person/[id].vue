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

const { t, locale } = useI18n()
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
    return new Date(dateStr).toLocaleDateString(locale.value === 'ru' ? 'ru-RU' : locale.value === 'ky' ? 'ky-KG' : 'en-US', {
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
  title: computed(() => person.value ? `${person.value.fullName} — ${t('header.title')}` : t('dashboard.view_record')),
})

// --- Озвучка (TTS) ---
const isSpeaking = ref(false)
let currentAudio: HTMLAudioElement | null = null

async function toggleSpeak() {
  if (isSpeaking.value) {
    if (currentAudio) {
      currentAudio.pause()
      currentAudio = null
    }
    isSpeaking.value = false
    return
  }

  if (!person.value?.biography) return

  isSpeaking.value = true

  try {
    // Запрашиваем аудио у нашего Nuxt-сервера (в формате Blob/Файла)
    const audioBlob = await $fetch<Blob>('/api/tts', {
      method: 'POST',
      body: { text: person.value.biography },
      responseType: 'blob' // Важно! Ожидаем бинарный файл, а не JSON
    })

    // Создаем временную ссылку на файл и проигрываем
    const audioUrl = URL.createObjectURL(audioBlob)
    currentAudio = new Audio(audioUrl)
    
    currentAudio.onended = () => {
      isSpeaking.value = false
      URL.revokeObjectURL(audioUrl)
      currentAudio = null
    }

    currentAudio.onerror = () => {
      isSpeaking.value = false
      if (currentAudio) URL.revokeObjectURL(currentAudio.src)
      currentAudio = null
    }

    await currentAudio.play()
  } catch (error) {
    console.error("Ошибка озвучки:", error)
    isSpeaking.value = false
  }
}

onUnmounted(() => {
  if (currentAudio) {
    currentAudio.pause()
    currentAudio = null
  }
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
        <span class="text-sm font-medium">{{ t('archive.person.back_to_list') }}</span>
      </NuxtLink>
    </nav>

    <!-- Ошибки / Загрузка -->
    <div v-if="isLoading" class="py-24 text-center">
      <p class="text-ink-muted animate-pulse font-serif italic text-lg">{{ t('archive.person.extracting_materials', { id: personId }) }}</p>
    </div>

    <div v-else-if="error" class="py-24 text-center border border-dashed border-accent/30 bg-paper-dark">
      <p class="text-accent mb-4 font-medium">{{ t('archive.person.not_found') }}</p>
      <NuxtLink to="/" class="text-ink underline decoration-dotted underline-offset-4">{{ t('archive.person.return_to_archive') }}</NuxtLink>
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
                <span class="text-[10px] uppercase tracking-[0.3em] px-2 py-1 bg-accent text-white">{{ t('archive.person.verified') }}</span>
                <p class="text-[10px] uppercase tracking-[0.2em] text-ink-muted font-mono">
                  Дело №{{ person.id }}
                </p>
                <!-- Бейдж статуса: если запись публична — она верифицирована -->
                <span class="inline-flex items-center gap-1.5 px-3 py-1 text-[10px] font-bold uppercase tracking-widest border text-verified border-verified bg-verified-bg/30">
                  <span class="w-1.5 h-1.5 bg-verified rounded-full" />
                  {{ t('archive.person.verified') }}
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
          <h2 class="text-[10px] uppercase tracking-[0.2em] text-ink-muted mb-6 md:mb-8 font-bold border-b border-border pb-1 flex items-center justify-between">
            <span>{{ t('archive.person.bio_summary') }}</span>
            <button 
              v-if="person.biography"
              class="flex items-center gap-1.5 px-3 py-1 -mt-1 text-[10px] text-ink-muted hover:text-accent transition-colors border border-transparent hover:border-accent/20 group/speak"
              :title="isSpeaking ? t('archive.person.stop_speaking') : t('archive.person.listen_bio')"
              @click="toggleSpeak"
            >
              <svg v-if="!isSpeaking" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="group-hover/speak:scale-110 transition-transform"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="animate-pulse"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>
              <span class="font-bold tracking-widest uppercase">{{ isSpeaking ? t('common.stop') : t('common.speak') }}</span>
            </button>
          </h2>
          <div class="prose prose-sm max-w-none text-ink leading-relaxed">
            <p v-if="person.biography" class="whitespace-pre-wrap text-base md:text-lg leading-loose font-serif italic text-ink-secondary">
              "{{ person.biography }}"
            </p>
            <p v-else class="text-ink-muted italic opacity-60">
              {{ t('archive.person.bio_digitizing') }}
            </p>
          </div>
        </section>

        <!-- Сведения о деле -->
        <section>
          <h2 class="text-[10px] uppercase tracking-[0.2em] text-ink-muted mb-6 md:mb-8 font-bold border-b border-border pb-1">
            {{ t('archive.person.case_summary') }}
          </h2>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-8 md:gap-x-12 gap-y-6 md:gap-y-8 bg-paper-dark/20 p-4 md:p-6 border border-border">
            <div>
              <dt class="text-[10px] text-ink-muted mb-1 uppercase tracking-widest">{{ t('archive.person.accusation_label') }}</dt>
              <dd class="text-sm md:text-base text-ink font-medium">{{ t(person.charge || person.accusation || '—') }}</dd>
            </div>
            <div>
              <dt class="text-[10px] text-ink-muted mb-1 uppercase tracking-widest">{{ t('archive.person.source_label') }}</dt>
              <dd class="text-sm md:text-base text-ink font-serif italic">{{ person.source || 'ИЦ ГКНБ' }}</dd>
            </div>
            <div>
              <dt class="text-[10px] text-ink-muted mb-1 uppercase tracking-widest">{{ t('archive.person.residence') }}</dt>
              <dd class="text-sm md:text-base text-ink">{{ t(person.region) }}{{ person.district ? `, ${person.district}` : '' }}</dd>
            </div>
            <div>
              <dt class="text-[10px] text-ink-muted mb-1 uppercase tracking-widest">{{ t('archive.person.occupation_label') }}</dt>
              <dd class="text-sm md:text-base text-ink">{{ person.occupation || t('archive.person.occupation_missing') }}</dd>
            </div>
            <div class="sm:col-span-2">
              <dt class="text-[10px] text-ink-muted mb-1 uppercase tracking-widest">{{ t('archive.person.sentence') }} ({{ t('archive.person.sentence_date_label') }})</dt>
              <dd class="text-sm md:text-base text-ink font-bold">{{ person.sentence || t('archive.person.sentence_missing') }}</dd>
            </div>
          </div>
        </section>

        <!-- Материалы дела (Реальные документы) -->
        <section>
          <div class="flex items-center justify-between mb-6 border-b border-border pb-1">
            <h2 class="text-[10px] uppercase tracking-[0.2em] text-ink-muted font-bold">
              {{ t('archive.person.materials_summary') }}
            </h2>
            <span v-if="person.documents?.length" class="text-[10px] text-ink-muted uppercase tracking-widest">
              {{ person.documents.length }} {{ t('archive.person.attached_docs').toLowerCase() }}
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
            <p class="text-sm text-ink-muted">{{ t('archive.person.no_docs') }}</p>
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
                {{ t('archive.person.chronology') }}
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
                  <p class="text-[10px] text-ink-muted uppercase mb-0.5">{{ t('archive.person.birth_label') }}</p>
                  <p class="text-sm font-serif font-bold">{{ person.birthYear }}</p>
                </li>
                <li class="relative pl-5 border-l border-border pb-1">
                  <span class="absolute left-[-4px] top-0 w-2 h-2 bg-border rounded-full" />
                  <p class="text-[10px] text-ink-muted uppercase mb-0.5">{{ t('archive.person.arrest_label') }}</p>
                  <p class="text-sm font-bold">{{ formatDate(person.arrestDate) }}</p>
                </li>
                <li class="relative pl-5 border-l border-border pb-1">
                  <span class="absolute left-[-4px] top-0 w-2 h-2 bg-border rounded-full" />
                  <p class="text-[10px] text-ink-muted uppercase mb-0.5">{{ t('archive.person.sentence_date_label') }}</p>
                  <p class="text-sm font-bold">{{ formatDate(person.sentenceDate) }}</p>
                </li>
                <li v-if="person.deathYear" class="relative pl-5 border-l border-border pb-1">
                  <span class="absolute left-[-4px] top-0 w-2 h-2 bg-ink rounded-full" />
                  <p class="text-[10px] text-ink-muted uppercase mb-0.5">{{ t('add.death_year') }}</p>
                  <p class="text-sm font-serif font-bold text-ink">{{ person.deathYear }}</p>
                </li>
                <li class="relative pl-5 border-l border-ink/30">
                  <span class="absolute left-[-4px] top-0 w-2 h-2 bg-verified rounded-full ring-2 ring-verified-bg" />
                  <p class="text-[10px] text-verified font-bold uppercase mb-0.5 tracking-widest">{{ t('archive.person.rehabilitation_label') }}</p>
                  <p class="text-sm md:text-base font-bold text-verified">{{ formatDate(person.rehabilitationDate) }}</p>
                </li>
              </ul>
            </div>
          </div>

          <!-- Контекстный чат (RAG) — ограниченная высота с внутренним скроллом -->
          <ContextChat
            :person-id="person.id"
            :title="t('archive.person.ai_search')"
            :subtitle="t('archive.person.ai_ask')"
            class="h-[350px] md:h-[400px] shadow-sm border border-border"
          />

          <!-- Техническая справка -->
          <div class="p-4 md:p-6 border border-border bg-ink text-white/90">
            <h4 class="text-[10px] font-bold uppercase tracking-widest mb-2">{{ t('archive.person.tech_info') }}</h4>
            <p class="text-[10px] md:text-[11px] leading-relaxed opacity-80">
              {{ t('archive.person.tech_desc') }}
            </p>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>
