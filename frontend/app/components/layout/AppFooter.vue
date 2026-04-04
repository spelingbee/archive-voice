<script setup lang="ts">
/**
 * components/layout/AppFooter.vue
 * Глобальный футер приложения.
 *
 * Содержит:
 * — Копирайт и описание проекта
 * — Упоминание КТМУ Манас
 * — Навигационные ссылки
 */
</script>

<template>
  <footer class="border-t border-border mt-16">
    <div
      class="max-w-7xl mx-auto px-4 md:px-8 py-8 md:py-10"
    >
      <!-- Основная строка -->
      <div class="flex flex-col md:flex-row items-center justify-between gap-6 text-sm text-ink-muted">
        <div class="text-center md:text-left space-y-1">
          <p>© {{ new Date().getFullYear() }} Голос из архива. Проект сохранения исторической памяти.</p>
          <p class="text-[10px] uppercase tracking-widest text-accent font-medium">
            Кыргызско-Турецкий университет «Манас» · Факультет компьютерных наук
          </p>
        </div>
        <nav class="flex items-center gap-4 md:gap-6 text-xs md:text-sm">
          <NuxtLink to="/about" class="hover:text-accent transition-colors">
            О проекте
          </NuxtLink>
          <NuxtLink to="/contribute" class="hover:text-accent transition-colors">
            Помочь проекту
          </NuxtLink>
          <NuxtLink to="/contact" class="hover:text-accent transition-colors">
            Контакты
          </NuxtLink>
        </nav>
      </div>
    </div>
  </footer>
</template>
