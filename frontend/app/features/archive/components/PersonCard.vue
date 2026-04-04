<script setup lang="ts">
/**
 * features/archive/components/PersonCard.vue
 * Карточка репрессированного — переиспользуемый компонент.
 *
 * Получает Person через props → не делает лишний API-запрос.
 * Роли берёт из useAuth() для ролевых кнопок.
 *
 * Отображает: ФИО, годы жизни, регион, обвинение, биографию (краткую).
 */
import type { Person } from '../types'
import { archiveRepository } from '../api'

const props = defineProps<{
  person: Person
}>()

const emit = defineEmits<{
  click: [person: Person]
}>()

const { t } = useI18n()

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
  props.person.status === 'verified' ? t('archive.person.verified') : t('archive.person.on_verification'),
)

/** Краткая биография (обрезается через CSS line-clamp) */
const shortBiography = computed(() =>
  props.person.biography || null,
)

// --- Действие верификации ---
const isVerifying = ref(false)

async function handleVerify() {
  if (!canVerify.value) return
  isVerifying.value = true
  try {
    // TODO: восстановить verify() в archiveRepository
    console.log('Verify:', props.person.id)
  } finally {
    isVerifying.value = false
  }
}
</script>

<template>
  <NuxtLink
    :to="`/person/${person.id}`"
    class="group relative block border border-border hover:border-border-hover transition-all bg-white overflow-hidden"
  >
    <!-- Акцентная полоска (университет Манас — тёмно-красная) -->
    <div class="absolute top-0 left-0 w-1 h-full bg-accent/40 group-hover:bg-accent transition-colors"></div>

    <div class="p-4 md:p-5 pl-5 md:pl-6">
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
        class="text-base md:text-lg font-semibold text-ink leading-tight pr-20 mb-1 group-hover:text-accent transition-colors"
        style="font-family: var(--font-serif)"
      >
        {{ person.fullName }}
      </h3>

      <!-- Годы жизни -->
      <p
        class="text-xs md:text-sm text-ink-muted mb-3"
        style="font-family: var(--font-mono)"
      >
        {{ formattedYears }}
      </p>

      <!-- Разделитель -->
      <hr class="border-border mb-3">

      <!-- Регион -->
      <p class="text-xs md:text-sm text-ink-secondary mb-1">
        {{ person.region }}
      </p>

      <!-- Обвинение -->
      <p class="text-xs md:text-sm text-ink-secondary font-medium mb-2">
        {{ person.accusation }}
      </p>

      <!-- Краткая биография (если есть) -->
      <p
        v-if="shortBiography"
        class="text-xs text-ink-muted leading-relaxed line-clamp-2 mb-2 italic"
      >
        {{ shortBiography }}
      </p>

      <!-- Приговор (если есть) -->
      <div v-if="person.sentence" class="mt-2 pt-2 border-t border-border/50">
        <p class="text-[10px] uppercase tracking-widest text-ink-muted mb-0.5">{{ t('archive.person.sentence') }}</p>
        <p class="text-xs text-ink font-medium">{{ person.sentence }}</p>
      </div>

      <!-- Кнопка верификации (только для ADMIN/MODERATOR и pending записей) -->
      <button
        v-if="canVerify"
        class="mt-3 inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium border border-verified text-verified hover:bg-verified-bg transition-colors"
        :disabled="isVerifying"
        @click.stop="handleVerify"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 6 9 17l-5-5" />
        </svg>
        {{ isVerifying ? t('archive.person.verifying') : t('archive.person.verify') }}
      </button>
    </div>
  </NuxtLink>
</template>
