/**
 * features/archive/composables/useArchivePerson.ts
 * Composable доменной логики для карточки репрессированного.
 *
 * Содержит:
 * - Загрузку данных через archiveRepository
 * - Бизнес-логику (форматирование, статус верификации)
 * - Проверку ролей для UI (canVerify, canEdit)
 * - Действие верификации
 *
 * Использование:
 *   const {
 *     person, isLoading, error,
 *     canVerify, canEdit,
 *     formattedYears, statusLabel,
 *     handleVerify
 *   } = useArchivePerson(personId)
 */

import type { PersonId } from '../types'
import { archiveRepository } from '../api'

export function useArchivePerson(id: MaybeRef<PersonId>) {
  // --- Data fetching ---
  const {
    data: person,
    status,
    error,
    refresh,
  } = archiveRepository.getById(id)

  // --- Состояния загрузки ---
  const isLoading = computed(() => status.value === 'pending')

  // --- Авторизация & роли ---
  const { isAdmin, isModerator, isAuthenticated } = useAuth()

  /** Может ли текущий пользователь верифицировать запись */
  const canVerify = computed(() => {
    if (!person.value) return false
    // Верифицировать могут только ADMIN и MODERATOR
    // и только записи в статусе 'pending'
    return (isAdmin.value || isModerator.value)
      && person.value.status === 'pending'
  })

  /** Может ли текущий пользователь редактировать запись */
  const canEdit = computed(() => {
    return isAuthenticated.value && (isAdmin.value || isModerator.value)
  })

  // --- Бизнес-логика (вычисляемые свойства) ---

  /** Форматированные годы жизни: "1899 — 1937" или "1899 — ?" */
  const formattedYears = computed(() => {
    if (!person.value) return ''
    const { birthYear, deathYear } = person.value
    return deathYear ? `${birthYear} — ${deathYear}` : `${birthYear} — ?`
  })

  /** Локализованный лейбл статуса верификации */
  const statusLabel = computed(() => {
    if (!person.value) return ''
    return person.value.status === 'verified'
      ? 'Верифицировано'
      : 'На проверке'
  })

  /** CSS-класс статуса для стилизации бейджа */
  const statusClass = computed(() => {
    if (!person.value) return ''
    return person.value.status === 'verified'
      ? 'text-verified'
      : 'text-ink-muted'
  })

  // --- Действия ---

  /** Верифицировать запись (мутация) */
  const isVerifying = ref(false)

  async function handleVerify(): Promise<void> {
    const currentId = toValue(id)
    if (!canVerify.value) return

    isVerifying.value = true
    try {
      await archiveRepository.verify(currentId)
      // Обновляем данные после успешной верификации
      await refresh()
    } finally {
      isVerifying.value = false
    }
  }

  return {
    // Data
    person: readonly(person),
    isLoading,
    error,

    // Permissions
    canVerify,
    canEdit,

    // Computed
    formattedYears,
    statusLabel,
    statusClass,

    // Actions
    handleVerify,
    isVerifying: readonly(isVerifying),
    refresh,
  }
}
