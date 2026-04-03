<script setup lang="ts">
/**
 * pages/index.vue
 * Главная страница — тонкий оркестратор.
 * Header/Footer/Chat теперь живут в app.vue.
 */
import { archiveRepository } from '~/features/archive/api'
import type { PersonFilters } from '~/features/archive/types'

// --- Поиск ---
const searchQuery = ref('')
const archiveFilters = ref<PersonFilters>({})

// Объединяем текстовый поиск и фильтры из ArchiveFilters
const combinedFilters = computed<PersonFilters>(() => ({
  search: searchQuery.value || undefined,
  ...archiveFilters.value,
}))

// --- Загрузка данных ---
const { data: personsPage, status } = archiveRepository.getList(combinedFilters.value)

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
    <!-- Поиск -->
    <section class="mb-10 md:mb-14">
      <div class="max-w-2xl mx-auto">
        <div class="relative">
          <div class="absolute left-0 top-1/2 -translate-y-1/2 text-ink-muted">
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
        </div>
      </div>
    </section>

    <!-- Фильтры -->
    <section class="mb-8">
      <ArchiveFilters @change="handleFiltersChange" />
    </section>

    <!-- Индикатор загрузки -->
    <div v-if="isLoading" class="text-center py-12 text-ink-muted">
      <p class="animate-pulse">Загрузка архива...</p>
    </div>

    <!-- Сетка карточек -->
    <section v-else>
      <h2 class="sr-only">Архив репрессированных</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 md:gap-6">
        <PersonCard
          v-for="person in persons"
          :key="person.id"
          :person="person"
        />
      </div>
    </section>

    <!-- Счётчик записей -->
    <div class="mt-16 text-center text-ink-muted text-sm">
      <p>
        Архив содержит
        <span class="font-medium text-ink">{{ totalCount }}</span> записей
      </p>
    </div>
  </div>
</template>
