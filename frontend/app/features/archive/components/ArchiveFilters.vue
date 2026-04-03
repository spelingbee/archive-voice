<script setup lang="ts">
/**
 * features/archive/components/ArchiveFilters.vue
 * Панель фильтров для архива.
 *
 * Использует shared constants и типы из домена archive.
 * Эмитит PersonFilters при каждом изменении фильтра.
 */
import type { PersonFilters } from '../types'
import { REGION_OPTIONS, ACCUSATION_OPTIONS, DECADE_OPTIONS } from '../constants'

const emit = defineEmits<{
  change: [filters: PersonFilters]
}>()

const selectedRegion = ref('')
const selectedDecade = ref('')
const selectedAccusationType = ref('')

const hasActiveFilters = computed(() =>
  selectedRegion.value !== ''
  || selectedDecade.value !== ''
  || selectedAccusationType.value !== '',
)

function emitFilters() {
  emit('change', {
    region: selectedRegion.value || undefined,
    decade: selectedDecade.value || undefined,
    accusationType: selectedAccusationType.value || undefined,
  })
}

function resetFilters() {
  selectedRegion.value = ''
  selectedDecade.value = ''
  selectedAccusationType.value = ''
  emitFilters()
}

watch([selectedRegion, selectedDecade, selectedAccusationType], emitFilters)
</script>

<template>
  <div class="flex flex-wrap items-center gap-4 md:gap-6">
    <!-- Region Filter -->
    <div class="relative">
      <label for="filter-region" class="sr-only">Фильтр по области</label>
      <select
        id="filter-region"
        v-model="selectedRegion"
        class="appearance-none bg-transparent border-0 border-b border-border text-sm text-ink py-2 pr-8 pl-0 focus:outline-none focus:border-ink cursor-pointer transition-colors"
      >
        <option
          v-for="region in REGION_OPTIONS"
          :key="region.value"
          :value="region.value"
        >
          {{ region.label }}
        </option>
      </select>
      <svg class="absolute right-0 top-1/2 -translate-y-1/2 pointer-events-none text-ink-muted" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="m6 9 6 6 6-6" />
      </svg>
    </div>

    <!-- Decade Filter -->
    <div class="relative">
      <label for="filter-decade" class="sr-only">Фильтр по десятилетию</label>
      <select
        id="filter-decade"
        v-model="selectedDecade"
        class="appearance-none bg-transparent border-0 border-b border-border text-sm text-ink py-2 pr-8 pl-0 focus:outline-none focus:border-ink cursor-pointer transition-colors"
      >
        <option
          v-for="decade in DECADE_OPTIONS"
          :key="decade.value"
          :value="decade.value"
        >
          {{ decade.label }}
        </option>
      </select>
      <svg class="absolute right-0 top-1/2 -translate-y-1/2 pointer-events-none text-ink-muted" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="m6 9 6 6 6-6" />
      </svg>
    </div>

    <!-- Accusation Type Filter -->
    <div class="relative">
      <label for="filter-accusation" class="sr-only">Фильтр по статье</label>
      <select
        id="filter-accusation"
        v-model="selectedAccusationType"
        class="appearance-none bg-transparent border-0 border-b border-border text-sm text-ink py-2 pr-8 pl-0 focus:outline-none focus:border-ink cursor-pointer transition-colors"
      >
        <option
          v-for="type in ACCUSATION_OPTIONS"
          :key="type.value"
          :value="type.value"
        >
          {{ type.label }}
        </option>
      </select>
      <svg class="absolute right-0 top-1/2 -translate-y-1/2 pointer-events-none text-ink-muted" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="m6 9 6 6 6-6" />
      </svg>
    </div>

    <!-- Reset Button -->
    <button
      v-if="hasActiveFilters"
      type="button"
      class="text-sm text-ink-muted hover:text-ink transition-colors"
      @click="resetFilters"
    >
      Сбросить
    </button>
  </div>
</template>
