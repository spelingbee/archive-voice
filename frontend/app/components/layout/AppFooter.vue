<script setup lang="ts">
/**
 * components/layout/AppFooter.vue
 * Глобальный футер приложения.
 */
</script>

<template>
  <footer class="border-t border-border mt-16">
    <div
      class="max-w-7xl mx-auto px-4 md:px-8 py-8 flex flex-col md:flex-row items-center justify-between gap-4 text-sm text-ink-muted"
    >
      <p>© {{ new Date().getFullYear() }} Голос из архива. Проект сохранения исторической памяти.</p>
      <nav class="flex items-center gap-6">
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
  </footer>
</template>
