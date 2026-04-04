import tailwindcss from "@tailwindcss/vite";

export default defineNuxtConfig({
  devtools: { enabled: true },
  modules: ["@nuxt/fonts", "@nuxtjs/i18n"],

  i18n: {
    locales: [
      { code: "ru", name: "Русский", file: "ru.json" },
      { code: "ky", name: "Кыргызча", file: "ky.json" },
      { code: "en", name: "English", file: "en.json" },
    ],
    langDir: "../i18n/locales/",
    defaultLocale: "ru",
    strategy: "no_prefix",
    detectBrowserLanguage: {
      useCookie: true,
      redirectOn: "root",
    },
    vueI18n: "./i18n.config.ts"
  },
  css: ["~/assets/css/main.css"],
  vite: {
    plugins: [tailwindcss()],
    server: {
      allowedHosts: true
    }
  },

  // --- Runtime Config ---
  runtimeConfig: {
    openaiKey: process.env.NUXT_OPENAI_KEY,
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
