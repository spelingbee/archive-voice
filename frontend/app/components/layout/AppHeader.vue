<script setup lang="ts">
/**
 * components/layout/AppHeader.vue
 * Глобальный хедер приложения.
 * Отвечает за навигацию, кнопки действий и авторизацию.
 *
 * Адаптив: На мобильных «Добавить» отображается как иконка-кнопка.
 */
const { isAuthenticated, user, isModerator, fullName, logout } = useAuth()

const emit = defineEmits<{
  openChat: []
}>()
</script>

<template>
  <header class="sticky top-0 z-50 bg-white border-b border-border px-4 md:px-8">
    <div class="max-w-7xl mx-auto h-14 flex items-center justify-between">
      <!-- Logo + Университет Манас -->
      <NuxtLink to="/" class="flex items-center gap-2 min-w-0">
        <h1
          class="text-lg md:text-2xl font-bold text-ink tracking-tight truncate"
          style="font-family: var(--font-serif)"
        >
          Голос из архива
        </h1>
        <span class="hidden sm:inline text-[9px] uppercase tracking-widest text-accent font-bold whitespace-nowrap">
          КТМУ Манас
        </span>
      </NuxtLink>

      <!-- Navigation -->
      <nav class="flex items-center gap-2 md:gap-5">
        <!-- Добавить запись: иконка на мобилках, полная кнопка на десктопе -->
        <NuxtLink
          to="/add"
          class="inline-flex items-center justify-center gap-2 px-2.5 py-2 md:px-4 text-sm font-medium text-ink border border-border hover:border-border-hover transition-colors"
          title="Добавить запись"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 5v14" />
            <path d="M5 12h14" />
          </svg>
          <span class="hidden md:inline">Добавить запись</span>
        </NuxtLink>

        <!-- Спросить ИИ -->
        <button
          class="flex items-center justify-center gap-2 px-2.5 py-2 md:px-3 text-sm font-medium text-ink border border-border hover:border-border-hover transition-colors"
          title="Спросить ИИ"
          @click="emit('openChat')"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          <span class="hidden md:inline">Спросить ИИ</span>
        </button>

        <!-- Авторизация / Профиль -->
        <template v-if="isAuthenticated">
          <!-- Ссылка на профиль: инициал на мобилках, имя на десктопе -->
          <NuxtLink
            to="/dashboard"
            class="inline-flex items-center gap-2 text-sm text-ink-secondary hover:text-ink transition-colors"
            title="Личный кабинет"
          >
            <!-- Аватар-инициал (всегда виден) -->
            <span class="w-7 h-7 rounded-full bg-accent text-white text-xs font-bold flex items-center justify-center">
              {{ fullName?.charAt(0)?.toUpperCase() || '?' }}
            </span>
            <!-- Имя (только десктоп) -->
            <span class="hidden md:inline">{{ fullName }}</span>
            <!-- Бейдж роли -->
            <span
              v-if="isModerator"
              class="hidden md:inline text-[10px] uppercase tracking-wide px-1.5 py-0.5 bg-paper-dark text-ink-muted border border-border"
            >
              {{ user?.role === 'ADMIN' ? 'Админ' : 'Модератор' }}
            </span>
          </NuxtLink>
          <button
            class="text-sm text-ink-secondary hover:text-accent transition-colors hidden md:inline"
            @click="logout"
          >
            Выйти
          </button>
        </template>
        <NuxtLink
          v-else
          to="/auth"
          class="text-sm text-ink-secondary hover:text-accent transition-colors whitespace-nowrap"
        >
          Войти
        </NuxtLink>
      </nav>
    </div>
  </header>
</template>
