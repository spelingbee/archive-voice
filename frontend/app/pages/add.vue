<script setup lang="ts">
/**
 * pages/add.vue
 * Форма добавления записи в архив.
 *
 * Особенности:
 * — Поддержка 13 полей архивного дела
 * — AI Smart Import (распознавание документов через ИИ)
 * — Загрузка файлов (доказательств) на бэкенд (/api/documents/upload)
 */
import type { PersonFormData } from '~/features/archive/types'
import { REGIONS, ACCUSATIONS } from '~/features/archive/constants'
import { archiveRepository } from '~/features/archive/api'

// --- SEO ---
useHead({ title: 'Добавить запись — Голос из архива' })

// --- Form state (13 полей) ---
const form = ref<PersonFormData>({
  fullName: '',
  birthYear: '',
  deathYear: '',
  region: '',
  district: '',
  occupation: '',
  accusation: '',      // Поле UI "Статья", мапится в charge
  arrestDate: '',      // Дата ареста
  sentence: '',        // Приговор
  sentenceDate: '',    // Дата приговора
  rehabilitationDate: '', // Дата реабилитации
  biography: '',
  source: '',          // Название/номер дела
})

// --- AI & File State ---
const selectedFile = ref<File | null>(null)
const isDragging = ref(false)
const isAIParsing = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

// --- Submit State ---
const isSubmitting = ref(false)
const submitError = ref<string | null>(null)
const submitSuccess = ref(false)
const uploadProgress = ref(0) // 0-100%

// --- File Handlers ---
function onFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    selectedFile.value = target.files[0] || null
  }
}

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
  if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
    selectedFile.value = event.dataTransfer.files[0] || null
  }
}

function triggerFileInput() {
  fileInput.value?.click()
}

// --- AI Parsing (Mock) ---
async function handleAIParse() {
  if (!selectedFile.value) return
  
  isAIParsing.value = true
  submitError.value = null

  try {
    // Имитация работы ИИ (3 секунды)
    await new Promise(resolve => setTimeout(resolve, 3000))

    // Заполнение формы моковыми данными "из документа"
    form.value = {
      fullName: 'Карасаев Кусеин Карасаевич',
      birthYear: '1901',
      deathYear: '1998',
      region: 'Иссык-Кульская область',
      district: 'Тюпский район',
      occupation: 'Лингвист, тюрколог, профессор',
      accusation: '58-10 (Контрреволюционная агитация)',
      arrestDate: '1937-11-10',
      sentence: '10 лет ИТЛ',
      sentenceDate: '1938-02-28',
      rehabilitationDate: '1954-06-15',
      biography: 'Выдающийся киргизский лингвист. Был арестован в 1937 году по ложному обвинению. Прошел лагеря, после чего был полностью реабилитирован и внес неоценимый вклад в науку.',
      source: 'Архив КНБ КР, дело №9982-К',
    }
  } catch (err) {
    submitError.value = 'Не удалось спарсить документ. Попробуйте заполнить вручную.'
  } finally {
    isAIParsing.value = false
  }
}

// --- Submit ---
async function handleSubmit() {
  isSubmitting.value = true
  submitError.value = null
  uploadProgress.value = 10

  try {
    // 1. Создаем запись персонажа
    const person = await archiveRepository.create({
      fullName: form.value.fullName,
      birthYear: Number(form.value.birthYear),
      deathYear: form.value.deathYear ? Number(form.value.deathYear) : null,
      region: form.value.region,
      district: form.value.district,
      occupation: form.value.occupation,
      charge: form.value.accusation, // Статья из формы
      arrestDate: form.value.arrestDate || null,
      sentence: form.value.sentence,
      sentenceDate: form.value.sentenceDate || null,
      rehabilitationDate: form.value.rehabilitationDate || null,
      biography: form.value.biography,
      source: form.value.source, // Название/номер дела
    })

    uploadProgress.value = 50

    // 2. Если есть файл — загружаем его
    if (selectedFile.value && person?.id) {
      await archiveRepository.uploadDocument(selectedFile.value, person.id, `Документ для дела ${person.fullName}`)
    }

    uploadProgress.value = 100
    submitSuccess.value = true
    setTimeout(() => navigateTo('/'), 2000)
  } catch (err: any) {
    submitError.value = err.response?._data?.message || 'Произошла ошибка при отправке. Проверьте поля.'
    uploadProgress.value = 0
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
      <p class="text-ink-muted leading-relaxed max-w-xl text-sm">
        Заполните анкету для добавления информации. Вы также можете использовать
        <span class="text-ink font-medium underline underline-offset-4 decoration-ink/20">Smart Import</span>
        для автоматического распознавания документов.
      </p>
    </header>

    <!-- Success Message -->
    <div
      v-if="submitSuccess"
      class="border border-verified bg-verified-bg p-6 text-center mb-8"
    >
      <p class="text-sm text-verified font-medium">
        ✓ Запись и документы успешно отправлены на модерацию. Перенаправляем...
      </p>
    </div>

    <!-- Error Message -->
    <div
      v-if="submitError"
      class="border border-accent/20 bg-red-50 p-4 mb-8"
    >
      <p class="text-sm text-accent">{{ submitError }}</p>
    </div>

    <div v-if="!submitSuccess" class="space-y-12">
      <!-- UI: Smart Import Zone -->
      <section>
        <h3 class="text-[10px] uppercase tracking-[0.2em] text-ink-muted mb-6 font-semibold px-1">
          Документ (Парсинг / Доказательство)
        </h3>
        <div
          class="relative border-2 border-dashed py-12 px-6 text-center transition-all duration-300 group"
          :class="[
            isDragging ? 'border-ink bg-paper-dark' : 'border-border-hover bg-white',
            selectedFile ? 'border-verified/30 bg-verified-bg/5' : ''
          ]"
          @dragover="handleDragOver"
          @dragleave="handleDragLeave"
          @drop="handleDrop"
        >
          <input
            ref="fileInput"
            type="file"
            class="hidden"
            accept=".pdf,.jpg,.jpeg,.png,.txt,.doc"
            @change="onFileSelect"
          />

          <div v-if="!selectedFile" class="flex flex-col items-center gap-4">
            <div class="w-12 h-12 rounded-full bg-paper-dark flex items-center justify-center text-ink-muted group-hover:scale-110 transition-transform">
               <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
            </div>
            <div>
              <p class="text-sm font-medium text-ink">Перетащите документ для распознавания</p>
              <p class="text-xs text-ink-muted mt-1">PDF, JPG, PNG или DOC до 10MB</p>
            </div>
            <button
              type="button"
              class="mt-2 text-xs font-bold uppercase tracking-widest text-ink hover:text-accent transition-colors"
              @click="triggerFileInput"
            >
              Выбрать вручную
            </button>
          </div>

          <div v-else class="flex flex-col items-center gap-4">
            <div class="flex items-center gap-3 bg-white border border-border px-4 py-2 shadow-sm">
               <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-verified"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>
               <span class="text-sm font-medium text-ink truncate max-w-[200px]">{{ selectedFile.name }}</span>
               <button @click="selectedFile = null" class="text-accent underline text-xs font-medium decoration-accent/30 hover:decoration-accent transition-all">Удалить</button>
            </div>
            
            <button
              type="button"
              class="px-10 py-3 bg-ink text-white text-[10px] font-bold uppercase tracking-[0.2em] hover:bg-ink/90 transition-all shadow-lg shadow-ink/10"
              @click="handleAIParse"
            >
              Заполнить через ИИ
            </button>
          </div>
        </div>
      </section>

      <!-- Main Form -->
      <form class="space-y-12" @submit.prevent="handleSubmit">
        <!-- 1. Личные данные -->
        <fieldset>
          <legend class="text-[10px] uppercase tracking-[0.2em] text-ink-muted mb-8 font-semibold">
            I. Личные данные
          </legend>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-x-10 gap-y-8">
            <div class="md:col-span-2">
              <label for="fullName" class="block text-xs text-ink-muted mb-2 uppercase tracking-wide">Фамилия Имя Отчество</label>
              <input
                id="fullName"
                v-model="form.fullName"
                type="text"
                required
                placeholder="Напр. Байтемиров Асан Жумабекович"
                class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
              />
            </div>

            <div>
              <label for="birthYear" class="block text-xs text-ink-muted mb-2 uppercase tracking-wide">Год рождения</label>
              <input
                id="birthYear"
                v-model="form.birthYear"
                type="text"
                required
                placeholder="1899"
                class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors font-mono"
              />
            </div>

            <div>
              <label for="deathYear" class="block text-xs text-ink-muted mb-2 uppercase tracking-wide">Год смерти</label>
              <input
                id="deathYear"
                v-model="form.deathYear"
                type="text"
                placeholder="1937"
                class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors font-mono"
              />
            </div>

            <div>
              <label for="region" class="block text-xs text-ink-muted mb-2 uppercase tracking-wide">Область</label>
              <select
                id="region"
                v-model="form.region"
                required
                class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink focus:border-ink focus:outline-none transition-colors appearance-none cursor-pointer"
                style="background-image: url('data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2212%22 height=%2212%22 viewBox=%220 0 24 24%22 fill=%22none%22 stroke=%22%236b7280%22 stroke-width=%222%22><path d=%22m6 9 6 6 6-6%22/></svg>'); background-repeat: no-repeat; background-position: right 0 center;"
              >
                <option value="" disabled>Выберите область</option>
                <option v-for="r in REGIONS" :key="r" :value="r">{{ r }}</option>
              </select>
            </div>

            <div>
              <label for="district" class="block text-xs text-ink-muted mb-2 uppercase tracking-wide">Район</label>
              <input
                id="district"
                v-model="form.district"
                type="text"
                placeholder="Аламединский р-н"
                class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
              />
            </div>

            <div class="md:col-span-2">
              <label for="occupation" class="block text-xs text-ink-muted mb-2 uppercase tracking-wide">Род занятий (профессия)</label>
              <input
                id="occupation"
                v-model="form.occupation"
                type="text"
                placeholder="Учитель, крестьянин, инженер..."
                class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
              />
            </div>
          </div>
        </fieldset>

        <!-- 2. Сведения о деле -->
        <fieldset>
          <legend class="text-[10px] uppercase tracking-[0.2em] text-ink-muted mb-8 font-semibold">
            II. Сведения о деле и приговоре
          </legend>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-x-10 gap-y-8">
            <div class="md:col-span-2">
              <label for="source" class="block text-xs text-ink-muted mb-2 uppercase tracking-wide">Архивный источник / Номер дела</label>
              <input
                id="source"
                v-model="form.source"
                required
                type="text"
                placeholder="Напр. Архив ГКНБ, дело №1234"
                class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
              />
            </div>

            <div>
              <label for="accusation" class="block text-xs text-ink-muted mb-2 uppercase tracking-wide">Статья обвинения</label>
              <select
                id="accusation"
                v-model="form.accusation"
                required
                class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink focus:border-ink focus:outline-none transition-colors appearance-none cursor-pointer"
                style="background-image: url('data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2212%22 height=%2212%22 viewBox=%220 0 24 24%22 fill=%22none%22 stroke=%22%236b7280%22 stroke-width=%222%22><path d=%22m6 9 6 6 6-6%22/></svg>'); background-repeat: no-repeat; background-position: right 0 center;"
              >
                <option value="" disabled>Выберите статью</option>
                <option v-for="a in ACCUSATIONS" :key="a" :value="a">{{ a }}</option>
              </select>
            </div>

            <div>
              <label for="arrestDate" class="block text-xs text-ink-muted mb-2 uppercase tracking-wide">Дата ареста</label>
              <input
                id="arrestDate"
                v-model="form.arrestDate"
                type="date"
                class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink focus:border-ink focus:outline-none transition-colors"
              />
            </div>

            <div class="md:col-span-2">
              <label for="sentence" class="block text-xs text-ink-muted mb-2 uppercase tracking-wide">Приговор (мера наказания)</label>
              <input
                id="sentence"
                v-model="form.sentence"
                required
                type="text"
                placeholder="Расстрел, 10 лет ИТЛ..."
                class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
              />
            </div>

            <div>
              <label for="sentenceDate" class="block text-xs text-ink-muted mb-2 uppercase tracking-wide">Дата приговора</label>
              <input
                id="sentenceDate"
                v-model="form.sentenceDate"
                type="date"
                class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink focus:border-ink focus:outline-none transition-colors"
              />
            </div>

            <div>
              <label for="rehabilitationDate" class="block text-xs text-ink-muted mb-2 uppercase tracking-wide">Дата реабилитации</label>
              <input
                id="rehabilitationDate"
                v-model="form.rehabilitationDate"
                type="date"
                class="w-full bg-transparent border-0 border-b border-border-hover py-2 text-ink focus:border-ink focus:outline-none transition-colors"
              />
            </div>
          </div>
        </fieldset>

        <!-- 3. Биография -->
        <fieldset>
          <legend class="text-[10px] uppercase tracking-[0.2em] text-ink-muted mb-8 font-semibold">
            III. Обстоятельства и биография
          </legend>
          <textarea
            v-model="form.biography"
            rows="6"
            placeholder="Опишите известные факты о жизни, обстоятельства ареста..."
            class="w-full bg-transparent border border-border p-4 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors resize-none leading-relaxed"
          ></textarea>
        </fieldset>

        <!-- Submit Section -->
        <div class="pt-10 flex flex-col items-center border-t border-border">
          <div v-if="isSubmitting" class="w-full max-w-xs mb-6 text-center">
             <div class="h-1 bg-paper-dark w-full overflow-hidden mb-2">
                <div class="h-full bg-ink transition-all duration-300" :style="{ width: `${uploadProgress}%` }"></div>
             </div>
             <span class="text-[9px] uppercase tracking-widest text-ink-muted">Сохранение записи и файлов...</span>
          </div>
          
          <button
            type="submit"
            :disabled="isSubmitting"
            class="px-16 py-4 bg-ink text-white text-xs font-bold uppercase tracking-[0.2em] hover:bg-ink/90 transition-all shadow-xl shadow-ink/10 disabled:opacity-50"
          >
            {{ isSubmitting ? 'Выполняется...' : 'Отправить на модерацию' }}
          </button>
          
          <p class="text-[10px] text-ink-muted mt-6 uppercase tracking-widest leading-relaxed text-center max-w-xs">
            Нажимая кнопку, вы подтверждаете достоверность данных.
          </p>
        </div>
      </form>
    </div>

    <!-- FULL SCREEN AI LOADER -->
    <Teleport to="body">
      <Transition
        enter-active-class="duration-300 ease-out"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="duration-500 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div v-if="isAIParsing" class="fixed inset-0 z-[100] bg-ink/95 flex items-center justify-center backdrop-blur-md">
           <div class="flex flex-col items-center max-w-sm w-full px-6 text-center">
              <!-- Animated Scanner Visual -->
              <div class="relative w-40 h-56 border border-white/20 mb-10 overflow-hidden">
                 <div class="absolute inset-0 bg-gradient-to-b from-transparent via-white/5 to-transparent"></div>
                 <div class="absolute top-0 left-0 right-0 h-1 bg-white/50 shadow-[0_0_20px_white] animate-scan"></div>
                 <div class="p-6 space-y-4 opacity-10">
                    <div class="h-1 w-full bg-white"></div><div class="h-1 w-2/3 bg-white"></div><div class="h-1 w-full bg-white"></div>
                 </div>
              </div>
              <h3 class="text-white text-xl font-serif italic mb-2 tracking-wide">Smart AI Analysis</h3>
              <p class="text-[10px] text-white/40 uppercase tracking-[0.4em] animate-pulse">Сканирование архивных материалов...</p>
           </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
@keyframes scan {
  0% { transform: translateY(0); }
  100% { transform: translateY(224px); }
}
.animate-scan {
  animation: scan 2s linear infinite;
}
</style>
