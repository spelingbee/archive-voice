import tailwindcss from "@tailwindcss/vite";

export default defineNuxtConfig({
  devtools: { enabled: true },
  modules: ["@nuxt/fonts"],
  css: ["~/assets/css/main.css"],
  vite: {
    plugins: [tailwindcss()],
  },

  // --- Runtime Config ---
  // Переменные доступны через useRuntimeConfig()
  // Переопределяются через .env: NUXT_PUBLIC_API_BASE=https://api.example.com
  runtimeConfig: {
    public: {
      apiBase: "https://jolie-causable-chloe.ngrok-free.dev/api",
    },
  },

  // --- Auto-imports из features ---
  // Composable из features/ автоматически доступны без import
  imports: {
    dirs: [
      "features/*/composables",
    ],
  },

  // --- Авто-регистрация компонентов из features ---
  components: [
    { path: "~/components", pathPrefix: false },
    { path: "~/features/archive/components", pathPrefix: false },
    { path: "~/features/chat/components", pathPrefix: false },
    { path: "~/features/auth/components", pathPrefix: false },
  ],

  app: {
    head: {
      title: "Голос из архива — Цифровой архив репрессированных",
      meta: [
        { charset: "utf-8" },
        { name: "viewport", content: "width=device-width, initial-scale=1" },
        {
          name: "description",
          content:
            "Цифровой исторический архив репрессированных. Поиск, документирование и сохранение памяти.",
        },
      ],
      htmlAttrs: {
        lang: "ru",
      },
    },
  },
});
