<script setup lang="ts">
/**
 * app.vue
 * Корневой layout: Header + Footer + ChatSlideOver, общие для ВСЕХ страниц.
 * Страницы рендерятся через <NuxtPage/> и отвечают только за свой контент.
 */
const isChatOpen = ref(false)
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