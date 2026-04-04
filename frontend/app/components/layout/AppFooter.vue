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
const { t } = useI18n()
</script>

<template>
  <footer class="border-t border-border mt-16">
    <div
      class="max-w-7xl mx-auto px-4 md:px-8 py-8 md:py-10"
    >
      <!-- Основная строка -->
      <div class="flex flex-col md:flex-row items-center justify-between gap-6 text-sm text-ink-muted">
        <div class="text-center md:text-left space-y-1">
          <p>{{ t('footer.copyright', { year: new Date().getFullYear() }) }}</p>
          <p class="text-[10px] uppercase tracking-widest text-accent font-medium">
            {{ t('footer.university_full') }}
          </p>
        </div>
        <nav class="flex items-center gap-4 md:gap-6 text-xs md:text-sm">
          <NuxtLink to="/about" class="hover:text-accent transition-colors">
            {{ t('footer.about') }}
          </NuxtLink>
          <NuxtLink to="/contribute" class="hover:text-accent transition-colors">
            {{ t('footer.contribute') }}
          </NuxtLink>
          <NuxtLink to="/contact" class="hover:text-accent transition-colors">
            {{ t('footer.contacts') }}
          </NuxtLink>
        </nav>
      </div>
    </div>
  </footer>
</template>
