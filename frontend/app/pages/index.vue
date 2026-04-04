<script setup lang="ts">
/**
 * pages/index.vue
 * Главная страница — точка входа в архивный реестр.
 * 
 * Включает поиск с дебаунсом (VueUse) и многофакторную фильтрацию.
 */
import { refDebounced } from '@vueuse/core'
import { archiveRepository } from '~/features/archive/api'
import type { PersonFilters } from '~/features/archive/types'

// --- Поиск ---
const searchQuery = ref('')
// Дебаунс 400мс: предотвращает излишнюю нагрузку при наборе имени
const debouncedQuery = refDebounced(searchQuery, 400)

// --- Фильтры ---
const archiveFilters = ref<PersonFilters>({})

/**
 * Объединяем текстовый поиск и фильтры.
 * Мы передаем сам computed-объект в репозиторий, чтобы useFetch следил за ним.
 */
const combinedFilters = computed(() => ({
  query: debouncedQuery.value || undefined,
  region: archiveFilters.value.region,
  year: archiveFilters.value.year?.toString(),
  charge: archiveFilters.value.charge,
}))

// --- Загрузка данных (Реактивно) ---
/**
 * Передаем combinedFilters (ComputedRef). 
 * Благодаря computed внутри api.ts, Nuxt будет атомарно следить за изменениями.
 */
const { data: personsPage, status } = archiveRepository.search(combinedFilters)

// Вспомогательные свойства
const persons = computed(() => personsPage.value?.content ?? [])
const totalCount = computed(() => personsPage.value?.totalElements ?? 0)
const isLoading = computed(() => status.value === 'pending')

/** Обработчик изменения фильтров */
function handleFiltersChange(filters: PersonFilters) {
  archiveFilters.value = filters
}
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 md:px-8 py-8 md:py-12">
    <!-- Секция поиска -->
    <section class="mb-10 md:mb-14">
      <div class="max-w-2xl mx-auto">
        <div class="relative group">
          <div class="absolute left-0 top-1/2 -translate-y-1/2 text-ink-muted group-focus-within:text-ink transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8" />
              <path d="m21 21-4.3-4.3" />
            </svg>
          </div>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Поиск по имени, региону, статье..."
            class="w-full pl-8 pr-4 py-3 text-lg bg-transparent border-0 border-b-2 border-border focus:border-ink focus:outline-none placeholder:text-ink-muted transition-colors"
          />
          <!-- Индикатор загрузки -->
          <div v-if="isLoading" class="absolute right-0 top-1/2 -translate-y-1/2">
             <div class="w-4 h-4 border-2 border-ink/20 border-t-ink rounded-full animate-spin"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- Секция фильтров -->
    <section class="mb-8">
      <ArchiveFilters @change="handleFiltersChange" />
    </section>

    <!-- Состояние загрузки -->
    <div v-if="isLoading && !persons.length" class="py-24 text-center">
       <p class="text-ink-muted font-serif italic animate-pulse text-lg">Загрузка материалов реестра...</p>
    </div>

    <!-- Список результатов -->
    <section v-else>
      <div v-if="persons.length">
        <h2 class="sr-only">Архивный реестр (результаты)</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 md:gap-6">
          <PersonCard
            v-for="person in persons"
            :key="person.id"
            :person="person"
          />
        </div>
      </div>
      
      <!-- Пустое состояние -->
      <div v-else class="py-24 text-center border border-dashed border-border-hover bg-paper-dark/20">
        <div class="mb-4 text-ink-muted opacity-30">
           <svg class="mx-auto" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
              <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/>
           </svg>
        </div>
        <p class="text-ink font-medium">Ничего не найдено</p>
        <p class="text-xs text-ink-muted mt-1 uppercase tracking-widest leading-loose">Попробуйте уточнить параметры или сбросить фильтры</p>
      </div>
    </section>

    <!-- Счётчик -->
    <div v-if="persons.length" class="mt-16 text-center text-ink-muted text-sm border-t border-border pt-8">
      <p class="tracking-[.25em] uppercase text-[10px] font-bold">
        В реестре обнаружено <span class="text-ink underline decoration-dotted underline-offset-4">{{ totalCount }}</span> записей
      </p>
    </div>
  </div>
</template>
