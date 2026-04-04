<script setup lang="ts">
/**
 * features/archive/components/ArchiveFilters.vue
 * Панель фильтров для архива.
 *
 * Использует shared constants и типы из домена archive.
 * Эмитит PersonFilters при каждом изменении фильтра.
 *
 * Адаптив: На мобильных фильтры горизонтально прокручиваются (scrollable row).
 */
import type { PersonFilters } from '../types'
import { REGION_OPTIONS, ACCUSATION_OPTIONS, DECADE_OPTIONS } from '../constants'

const emit = defineEmits<{
  change: [filters: PersonFilters]
}>()

// --- Внутреннее состояние фильтров ---
const selectedRegion = ref('')
const selectedDecade = ref('')
const selectedAccusationType = ref('')
const selectedYear = ref('')

/** Вычисляемое свойство для кнопки сброса */
const hasActiveFilters = computed(() =>
  selectedRegion.value !== ''
  || selectedDecade.value !== ''
  || selectedAccusationType.value !== ''
  || selectedYear.value !== '',
)

/**
 * Генерирует объект фильтров и пробрасывает его наверх.
 * Поля 'year' и 'charge' маппятся специально для эндпоинта /search.
 */
function emitFilters() {
  emit('change', {
    region: selectedRegion.value || undefined,
    decade: selectedDecade.value || undefined,
    accusationType: selectedAccusationType.value || undefined,
    year: selectedYear.value || undefined,
    charge: selectedAccusationType.value || undefined, // Мапим статью в charge
  })
}

/** Сброс всех значений в исходное состояние */
function resetFilters() {
  selectedRegion.value = ''
  selectedDecade.value = ''
  selectedAccusationType.value = ''
  selectedYear.value = ''
  emitFilters()
}

// Следим за изменениями любого фильтра и уведомляем родителя
watch([selectedRegion, selectedDecade, selectedAccusationType, selectedYear], (_) => {
  emitFilters()
})
</script>

<template>
  <!-- Горизонтальный скролл на мобильных, обычный flex-wrap на десктопе -->
  <div class="flex items-center gap-3 md:gap-5 overflow-x-auto md:overflow-visible pb-2 md:pb-0 -mx-4 px-4 md:mx-0 md:px-0 scrollbar-none">
    <!-- Region Filter -->
    <div class="relative shrink-0">
      <label for="filter-region" class="sr-only">Фильтр по области</label>
      <select
        id="filter-region"
        v-model="selectedRegion"
        class="appearance-none bg-transparent border-0 border-b border-border text-xs md:text-sm text-ink py-1.5 md:py-2 pr-6 pl-0 focus:outline-none focus:border-ink cursor-pointer transition-colors whitespace-nowrap"
      >
        <option
          v-for="region in REGION_OPTIONS"
          :key="region.value"
          :value="region.value"
        >
          {{ region.label }}
        </option>
      </select>
      <svg class="absolute right-0 top-1/2 -translate-y-1/2 pointer-events-none text-ink-muted" xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="m6 9 6 6 6-6" />
      </svg>
    </div>

    <!-- Accusation Type Filter -->
    <div class="relative shrink-0">
      <label for="filter-accusation" class="sr-only">Фильтр по статье</label>
      <select
        id="filter-accusation"
        v-model="selectedAccusationType"
        class="appearance-none bg-transparent border-0 border-b border-border text-xs md:text-sm text-ink py-1.5 md:py-2 pr-6 pl-0 focus:outline-none focus:border-ink cursor-pointer transition-colors whitespace-nowrap"
      >
        <option
          v-for="type in ACCUSATION_OPTIONS"
          :key="type.value"
          :value="type.value"
        >
          {{ type.label }}
        </option>
      </select>
      <svg class="absolute right-0 top-1/2 -translate-y-1/2 pointer-events-none text-ink-muted" xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="m6 9 6 6 6-6" />
      </svg>
    </div>

    <!-- Year Filter -->
    <div class="relative shrink-0 w-16 md:w-20">
      <label for="filter-year" class="sr-only">Год</label>
      <input
        id="filter-year"
        v-model="selectedYear"
        type="text"
        inputmode="numeric"
        placeholder="Год..."
        class="bg-transparent border-0 border-b border-border text-xs md:text-sm text-ink py-1.5 md:py-2 w-full focus:outline-none focus:border-ink transition-colors font-mono placeholder:text-ink-muted"
      />
    </div>

    <!-- Reset Button -->
    <button
      v-if="hasActiveFilters"
      type="button"
      class="text-xs md:text-sm text-ink-muted hover:text-ink transition-colors shrink-0 whitespace-nowrap"
      @click="resetFilters"
    >
      Сбросить
    </button>
  </div>
</template>

<style scoped>
/* Убираем скроллбар на мобильных */
.scrollbar-none::-webkit-scrollbar {
  display: none;
}
.scrollbar-none {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
