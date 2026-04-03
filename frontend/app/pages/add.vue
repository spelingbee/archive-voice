<script setup lang="ts">
/**
 * pages/add.vue
 * Форма добавления записи в архив.
 *
 * Использует:
 * — типы из features/archive/types (PersonFormData)
 * — константы из features/archive/constants (REGIONS, ACCUSATIONS)
 * — archiveRepository.create() для отправки на сервер
 * — Layout из app.vue (Header/Footer автоматически)
 */
import type { PersonFormData } from '~/features/archive/types'
import { REGIONS, ACCUSATIONS } from '~/features/archive/constants'
import { archiveRepository } from '~/features/archive/api'

// --- SEO ---
useHead({ title: 'Добавить запись — Голос из архива' })

// --- Form state ---
const form = ref<PersonFormData>({
  fullName: '',
  birthYear: '',
  deathYear: '',
  region: '',
  district: '',
  profession: '',
  accusation: '',
  biography: '',
})

const isDragging = ref(false)
const isSubmitting = ref(false)
const submitError = ref<string | null>(null)
const submitSuccess = ref(false)

// --- Drag & Drop ---
function handleDragOver(event: DragEvent) {
  event.preventDefault()
  isDragging.value = true
}

function handleDragLeave() {
  isDragging.value = false
}

function handleDrop(event: DragEvent) {
  event.preventDefault()
  isDragging.value = false
  // TODO: обработка файлов из event.dataTransfer.files
}

// --- Submit ---
async function handleSubmit() {
  isSubmitting.value = true
  submitError.value = null

  try {
    await archiveRepository.create({
      fullName: form.value.fullName,
      birthYear: Number(form.value.birthYear),
      deathYear: form.value.deathYear ? Number(form.value.deathYear) : null,
      region: form.value.region,
      accusation: form.value.accusation,
    })

    submitSuccess.value = true

    // Редирект на главную через 2 секунды
    setTimeout(() => navigateTo('/'), 2000)
  } catch (err: unknown) {
    submitError.value = err instanceof Error
      ? err.message
      : 'Произошла ошибка при отправке. Попробуйте ещё раз.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="max-w-3xl mx-auto px-4 md:px-8 py-10 md:py-16">
    <!-- Page Header -->
    <header class="mb-10 md:mb-14">
      <NuxtLink
        to="/"
        class="inline-flex items-center gap-1 text-sm text-ink-muted hover:text-ink transition-colors mb-6"
      >
        ← Вернуться к архиву
      </NuxtLink>

      <h2
        class="text-2xl md:text-3xl font-semibold text-ink mb-3"
        style="font-family: var(--font-serif)"
      >
        Внесение данных в архив
      </h2>
      <p class="text-ink-muted leading-relaxed max-w-xl">
        Заполните анкету для добавления информации о репрессированном.
        После модерации запись появится в открытом доступе.
      </p>
    </header>

    <!-- Success Message -->
    <div
      v-if="submitSuccess"
      class="border border-verified bg-verified-bg p-6 text-center mb-8"
    >
      <p class="text-sm text-verified font-medium">
        ✓ Запись успешно отправлена на модерацию. Перенаправляем...
      </p>
    </div>

    <!-- Error Message -->
    <div
      v-if="submitError"
      class="border border-accent/30 bg-red-50 p-4 mb-8"
    >
      <p class="text-sm text-accent">{{ submitError }}</p>
    </div>

    <!-- Form -->
    <form v-if="!submitSuccess" class="space-y-10" @submit.prevent="handleSubmit">
      <!-- Personal Information -->
      <fieldset>
        <legend class="text-xs uppercase tracking-widest text-ink-muted mb-6 font-medium">
          Личные данные
        </legend>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
          <!-- Full Name -->
          <div class="md:col-span-2">
            <label for="fullName" class="block text-sm text-ink-secondary mb-2">
              Фамилия, имя, отчество
            </label>
            <input
              id="fullName"
              v-model="form.fullName"
              type="text"
              required
              placeholder="Байтемиров Асан Жумабекович"
              class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
            />
          </div>

          <!-- Birth Year -->
          <div>
            <label for="birthYear" class="block text-sm text-ink-secondary mb-2">
              Год рождения
            </label>
            <input
              id="birthYear"
              v-model="form.birthYear"
              type="text"
              inputmode="numeric"
              pattern="[0-9]{4}"
              placeholder="1899"
              class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
              style="font-family: var(--font-mono)"
            />
          </div>

          <!-- Death Year -->
          <div>
            <label for="deathYear" class="block text-sm text-ink-secondary mb-2">
              Год смерти
              <span class="text-ink-muted">(если известен)</span>
            </label>
            <input
              id="deathYear"
              v-model="form.deathYear"
              type="text"
              inputmode="numeric"
              pattern="[0-9]{4}"
              placeholder="1937"
              class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
              style="font-family: var(--font-mono)"
            />
          </div>

          <!-- Region -->
          <div>
            <label for="region" class="block text-sm text-ink-secondary mb-2">Область</label>
            <select
              id="region"
              v-model="form.region"
              class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink focus:border-ink focus:outline-none transition-colors cursor-pointer appearance-none"
              style="background-image: url('data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2212%22 height=%2212%22 viewBox=%220 0 24 24%22 fill=%22none%22 stroke=%22%236b7280%22 stroke-width=%222%22><path d=%22m6 9 6 6 6-6%22/></svg>'); background-repeat: no-repeat; background-position: right 0 center;"
            >
              <option value="" disabled>Выберите область</option>
              <option v-for="r in REGIONS" :key="r" :value="r">{{ r }}</option>
            </select>
          </div>

          <!-- District -->
          <div>
            <label for="district" class="block text-sm text-ink-secondary mb-2">
              Район <span class="text-ink-muted">(если известен)</span>
            </label>
            <input
              id="district"
              v-model="form.district"
              type="text"
              placeholder="Аламединский район"
              class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
            />
          </div>

          <!-- Profession -->
          <div>
            <label for="profession" class="block text-sm text-ink-secondary mb-2">
              Профессия / род занятий
            </label>
            <input
              id="profession"
              v-model="form.profession"
              type="text"
              placeholder="Учитель, колхозник, инженер..."
              class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
            />
          </div>

          <!-- Accusation -->
          <div>
            <label for="accusation" class="block text-sm text-ink-secondary mb-2">
              Статья обвинения
            </label>
            <select
              id="accusation"
              v-model="form.accusation"
              class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink focus:border-ink focus:outline-none transition-colors cursor-pointer appearance-none"
              style="background-image: url('data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2212%22 height=%2212%22 viewBox=%220 0 24 24%22 fill=%22none%22 stroke=%22%236b7280%22 stroke-width=%222%22><path d=%22m6 9 6 6 6-6%22/></svg>'); background-repeat: no-repeat; background-position: right 0 center;"
            >
              <option value="" disabled>Выберите статью</option>
              <option v-for="a in ACCUSATIONS" :key="a" :value="a">{{ a }}</option>
            </select>
          </div>
        </div>
      </fieldset>

      <!-- Biography -->
      <fieldset>
        <legend class="text-xs uppercase tracking-widest text-ink-muted mb-6 font-medium">
          Биография
        </legend>
        <div>
          <label for="biography" class="block text-sm text-ink-secondary mb-2">
            Краткая биография и обстоятельства ареста
          </label>
          <textarea
            id="biography"
            v-model="form.biography"
            rows="6"
            placeholder="Опишите известные факты о жизни человека, обстоятельства ареста, приговор, место содержания, дальнейшую судьбу..."
            class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors resize-none leading-relaxed"
          />
        </div>
      </fieldset>

      <!-- Document Upload -->
      <fieldset>
        <legend class="text-xs uppercase tracking-widest text-ink-muted mb-6 font-medium">
          Документы
        </legend>
        <div
          class="border-2 border-dashed py-12 px-6 text-center transition-colors"
          :class="isDragging ? 'border-ink bg-paper-dark' : 'border-border-hover bg-paper-dark/50'"
          @dragover="handleDragOver"
          @dragleave="handleDragLeave"
          @drop="handleDrop"
        >
          <div class="flex flex-col items-center gap-3">
            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="text-ink-muted">
              <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
              <polyline points="14 2 14 8 20 8" />
              <path d="M12 18v-6" />
              <path d="m9 15 3-3 3 3" />
            </svg>
            <div>
              <p class="text-ink-secondary text-sm mb-1">Перетащите сканы документов сюда</p>
              <p class="text-ink-muted text-xs">
                или
                <button type="button" class="text-ink underline underline-offset-2 hover:text-accent transition-colors">
                  нажмите для выбора
                </button>
              </p>
            </div>
            <p class="text-[11px] text-ink-muted mt-2" style="font-family: var(--font-mono)">
              PDF, TXT, MD, JPG, PNG — до 10 МБ
            </p>
          </div>
        </div>
      </fieldset>

      <!-- Submit -->
      <div class="pt-6 border-t border-border">
        <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <p class="text-xs text-ink-muted max-w-sm leading-relaxed">
            Нажимая кнопку, вы подтверждаете достоверность предоставленной информации
            и соглашаетесь с правилами публикации.
          </p>
          <button
            type="submit"
            :disabled="isSubmitting"
            class="px-6 py-3 bg-ink text-white text-sm font-medium hover:bg-ink/90 transition-colors flex-shrink-0 disabled:opacity-50"
          >
            {{ isSubmitting ? 'Отправка...' : 'Отправить на модерацию' }}
          </button>
        </div>
      </div>
    </form>
  </div>
</template>
