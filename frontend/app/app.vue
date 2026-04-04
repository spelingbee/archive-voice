<script setup lang="ts">
/**
 * app.vue
 * Корневой layout: Header + Footer + ChatSlideOver, общие для ВСЕХ страниц.
 * Страницы рендерятся через <NuxtPage/> и отвечают только за свой контент.
 */
const { t } = useI18n()
const isChatOpen = ref(false)

useHead({
  title: t('header.title'),
  meta: [
    { name: 'description', content: t('footer.copyright', { year: new Date().getFullYear() }) }
  ]
})
</script>

<template>
  <div class="min-h-screen bg-paper flex flex-col">
    <AppHeader @open-chat="isChatOpen = true" />

    <main class="flex-1">
      <NuxtPage />
    </main>

    <AppFooter />

    <!-- Global Chat Slide-over (доступен с любой страницы) -->
    <ChatSlideOver :open="isChatOpen" @close="isChatOpen = false" />
  </div>
</template>