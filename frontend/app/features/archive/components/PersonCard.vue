<script setup lang="ts">
/**
 * features/archive/components/PersonCard.vue
 * Карточка репрессированного — переиспользуемый компонент.
 *
 * Получает Person через props → не делает лишний API-запрос.
 * Роли берёт из useAuth() для ролевых кнопок.
 */
import type { Person } from '../types'
import { archiveRepository } from '../api'

const props = defineProps<{
  person: Person
}>()

const emit = defineEmits<{
  click: [person: Person]
}>()

// --- Роли из auth (не из useArchivePerson — тот делает API-запрос) ---
const { isAdmin, isModerator } = useAuth()

/** Может ли текущий пользователь верифицировать ИМЕННО ЭТУ запись */
const canVerify = computed(() =>
  (isAdmin.value || isModerator.value) && props.person.status === 'pending',
)

/** Форматированные годы жизни */
const formattedYears = computed(() => {
  const { birthYear, deathYear } = props.person
  return deathYear ? `${birthYear} — ${deathYear}` : `${birthYear} — ?`
})

/** Лейбл статуса */
const statusLabel = computed(() =>
  props.person.status === 'verified' ? 'Верифицировано' : 'На проверке',
)

// --- Действие верификации ---
const isVerifying = ref(false)

async function handleVerify() {
  if (!canVerify.value) return
  isVerifying.value = true
  try {
    await archiveRepository.verify(props.person.id)
    // TODO: обновить карточку после верификации (emit или refresh)
  } finally {
    isVerifying.value = false
  }
}
</script>

<template>
  <NuxtLink
    :to="`/person/${person.id}`"
    class="group relative block border border-border hover:border-border-hover transition-colors bg-white"
  >
    <div class="p-4 md:p-5">
      <!-- Статус верификации (бейдж) -->
      <div
        v-if="person.status === 'verified'"
        class="absolute top-3 right-3 flex items-center gap-1.5"
      >
        <span class="w-2 h-2 rounded-full bg-verified" />
        <span class="text-[10px] uppercase tracking-wide text-verified font-medium">
          {{ statusLabel }}
        </span>
      </div>

      <!-- ФИО -->
      <h3
        class="text-lg md:text-xl font-semibold text-ink leading-tight pr-24 mb-1"
        style="font-family: var(--font-serif)"
      >
        {{ person.fullName }}
      </h3>

      <!-- Годы жизни -->
      <p
        class="text-sm text-ink-muted mb-4"
        style="font-family: var(--font-mono)"
      >
        {{ formattedYears }}
      </p>

      <!-- Разделитель -->
      <hr class="border-border mb-4">

      <!-- Регион -->
      <p class="text-sm text-ink-secondary mb-2">
        {{ person.region }}
      </p>

      <!-- Обвинение -->
      <p class="text-sm text-ink-secondary line-clamp-2">
        {{ person.accusation }}
      </p>

      <!-- Кнопка верификации (только для ADMIN/MODERATOR и pending записей) -->
      <button
        v-if="canVerify"
        class="mt-4 inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium border border-verified text-verified hover:bg-verified-bg transition-colors"
        :disabled="isVerifying"
        @click.stop="handleVerify"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 6 9 17l-5-5" />
        </svg>
        {{ isVerifying ? 'Верификация...' : 'Верифицировать' }}
      </button>
    </div>
  </NuxtLink>
</template>
