<script setup lang="ts">
/**
 * components/layout/AppHeader.vue
 * Глобальный хедер приложения.
 * Отвечает за навигацию, кнопки действий и авторизацию.
 */
const { isAuthenticated, user, isModerator, logout } = useAuth()

const emit = defineEmits<{
  openChat: []
}>()
</script>

<template>
  <header class="sticky top-0 z-50 bg-white border-b border-border px-4 md:px-8">
    <div class="max-w-7xl mx-auto h-14 flex items-center justify-between">
      <!-- Logo -->
      <NuxtLink to="/" class="flex items-center gap-2">
        <h1
          class="text-xl md:text-2xl font-bold text-ink tracking-tight"
          style="font-family: var(--font-serif)"
        >
          Голос из архива
        </h1>
      </NuxtLink>

      <!-- Navigation -->
      <nav class="flex items-center gap-4 md:gap-6">
        <!-- Добавить запись -->
        <NuxtLink
          to="/add"
          class="hidden md:inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-ink border border-border hover:border-border-hover transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 5v14" />
            <path d="M5 12h14" />
          </svg>
          Добавить запись
        </NuxtLink>

        <!-- Спросить ИИ -->
        <button
          class="flex items-center gap-2 px-3 py-2 text-sm font-medium text-ink border border-border hover:border-border-hover transition-colors"
          @click="emit('openChat')"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          <span class="hidden md:inline">Спросить ИИ</span>
        </button>

        <!-- Авторизация / Профиль -->
        <template v-if="isAuthenticated">
          <!-- Имя пользователя → ссылка на личный кабинет -->
          <NuxtLink
            to="/dashboard"
            class="hidden md:inline-flex items-center gap-2 text-sm text-ink-secondary hover:text-ink transition-colors"
          >
            {{ user?.fullName }}
            <!-- Бейдж роли -->
            <span
              v-if="isModerator"
              class="text-[10px] uppercase tracking-wide px-1.5 py-0.5 bg-paper-dark text-ink-muted border border-border"
            >
              {{ user?.role === 'ADMIN' ? 'Админ' : 'Модератор' }}
            </span>
          </NuxtLink>
          <button
            class="text-sm text-ink-secondary hover:text-accent transition-colors"
            @click="logout"
          >
            Выйти
          </button>
        </template>
        <NuxtLink
          v-else
          to="/auth"
          class="text-sm text-ink-secondary hover:text-accent transition-colors"
        >
          Войти / Регистрация
        </NuxtLink>
      </nav>
    </div>
  </header>
</template>
