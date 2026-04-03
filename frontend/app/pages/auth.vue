<script setup lang="ts">
/**
 * pages/auth.vue
 * Страница авторизации — вход и регистрация.
 *
 * Использует useAuth() для login/register.
 * При успехе → редирект на / (или на предыдущую страницу).
 */

// --- SEO ---
useHead({ title: 'Вход — Голос из архива' })

// --- Auth ---
const { isAuthenticated, login, register } = useAuth()

// Если уже авторизован → на главную
watch(isAuthenticated, (val) => {
  if (val) navigateTo('/')
}, { immediate: true })

// --- Tabs ---
type AuthTab = 'login' | 'register'
const activeTab = ref<AuthTab>('login')

// --- Login form ---
const loginForm = ref({
  email: '',
  password: '',
})

// --- Register form ---
const registerForm = ref({
  fullName: '',
  email: '',
  password: '',
  confirmPassword: '',
})

// --- State ---
const isLoading = ref(false)
const errorMessage = ref<string | null>(null)

// --- Validation ---
const passwordsMatch = computed(() =>
  registerForm.value.password === registerForm.value.confirmPassword,
)

// --- Actions ---
async function handleLogin() {
  errorMessage.value = null
  isLoading.value = true

  try {
    await login({
      email: loginForm.value.email,
      password: loginForm.value.password,
    })
    // Редирект произойдёт через watch(isAuthenticated)
  } catch (err: unknown) {
    errorMessage.value = err instanceof Error
      ? err.message
      : 'Неверный email или пароль. Проверьте данные и попробуйте снова.'
  } finally {
    isLoading.value = false
  }
}

async function handleRegister() {
  errorMessage.value = null

  if (!passwordsMatch.value) {
    errorMessage.value = 'Пароли не совпадают'
    return
  }

  isLoading.value = true

  try {
    await register({
      fullName: registerForm.value.fullName,
      email: registerForm.value.email,
      password: registerForm.value.password,
    })
  } catch (err: unknown) {
    errorMessage.value = err instanceof Error
      ? err.message
      : 'Ошибка при регистрации. Возможно, этот email уже используется.'
  } finally {
    isLoading.value = false
  }
}

/** Переключение вкладки со сбросом ошибки */
function switchTab(tab: AuthTab) {
  activeTab.value = tab
  errorMessage.value = null
}
</script>

<template>
  <div class="flex items-center justify-center min-h-[calc(100vh-8rem)] px-4 py-12">
    <div class="w-full max-w-md">
      <!-- Header -->
      <div class="text-center mb-10">
        <h2
          class="text-2xl md:text-3xl font-semibold text-ink mb-2"
          style="font-family: var(--font-serif)"
        >
          {{ activeTab === 'login' ? 'Вход в архив' : 'Регистрация' }}
        </h2>
        <p class="text-sm text-ink-muted">
          {{ activeTab === 'login'
            ? 'Войдите, чтобы добавлять записи и работать с архивом'
            : 'Создайте аккаунт исследователя'
          }}
        </p>
      </div>

      <!-- Tab Switcher -->
      <div class="flex border-b border-border mb-8">
        <button
          class="flex-1 pb-3 text-sm font-medium transition-colors border-b-2 -mb-px"
          :class="activeTab === 'login'
            ? 'border-ink text-ink'
            : 'border-transparent text-ink-muted hover:text-ink'
          "
          @click="switchTab('login')"
        >
          Вход
        </button>
        <button
          class="flex-1 pb-3 text-sm font-medium transition-colors border-b-2 -mb-px"
          :class="activeTab === 'register'
            ? 'border-ink text-ink'
            : 'border-transparent text-ink-muted hover:text-ink'
          "
          @click="switchTab('register')"
        >
          Регистрация
        </button>
      </div>

      <!-- Error Message -->
      <div
        v-if="errorMessage"
        class="mb-6 border border-accent/30 bg-red-50 px-4 py-3"
      >
        <p class="text-sm text-accent">{{ errorMessage }}</p>
      </div>

      <!-- Login Form -->
      <form
        v-if="activeTab === 'login'"
        class="space-y-6"
        @submit.prevent="handleLogin"
      >
        <div>
          <label for="login-email" class="block text-sm text-ink-secondary mb-2">
            Email
          </label>
          <input
            id="login-email"
            v-model="loginForm.email"
            type="email"
            required
            autocomplete="email"
            placeholder="researcher@archive.kg"
            class="w-full bg-transparent border-0 border-b border-border-hover py-2.5 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
          />
        </div>

        <div>
          <label for="login-password" class="block text-sm text-ink-secondary mb-2">
            Пароль
          </label>
          <input
            id="login-password"
            v-model="loginForm.password"
            type="password"
            required
            autocomplete="current-password"
            placeholder="••••••••"
            class="w-full bg-transparent border-0 border-b border-border-hover py-2.5 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
          />
        </div>

        <button
          type="submit"
          :disabled="isLoading"
          class="w-full py-3 bg-ink text-white text-sm font-medium hover:bg-ink/90 transition-colors disabled:opacity-50"
        >
          {{ isLoading ? 'Вход...' : 'Войти' }}
        </button>

        <p class="text-center text-xs text-ink-muted">
          Нет аккаунта?
          <button
            type="button"
            class="text-ink underline underline-offset-2 hover:text-accent transition-colors"
            @click="switchTab('register')"
          >
            Зарегистрируйтесь
          </button>
        </p>
      </form>

      <!-- Register Form -->
      <form
        v-if="activeTab === 'register'"
        class="space-y-6"
        @submit.prevent="handleRegister"
      >
        <div>
          <label for="reg-name" class="block text-sm text-ink-secondary mb-2">
            Полное имя
          </label>
          <input
            id="reg-name"
            v-model="registerForm.fullName"
            type="text"
            required
            autocomplete="name"
            placeholder="Иванов Иван Иванович"
            class="w-full bg-transparent border-0 border-b border-border-hover py-2.5 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
          />
        </div>

        <div>
          <label for="reg-email" class="block text-sm text-ink-secondary mb-2">
            Email
          </label>
          <input
            id="reg-email"
            v-model="registerForm.email"
            type="email"
            required
            autocomplete="email"
            placeholder="researcher@archive.kg"
            class="w-full bg-transparent border-0 border-b border-border-hover py-2.5 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
          />
        </div>

        <div>
          <label for="reg-password" class="block text-sm text-ink-secondary mb-2">
            Пароль
          </label>
          <input
            id="reg-password"
            v-model="registerForm.password"
            type="password"
            required
            autocomplete="new-password"
            minlength="8"
            placeholder="Минимум 8 символов"
            class="w-full bg-transparent border-0 border-b border-border-hover py-2.5 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
          />
        </div>

        <div>
          <label for="reg-confirm" class="block text-sm text-ink-secondary mb-2">
            Подтвердите пароль
          </label>
          <input
            id="reg-confirm"
            v-model="registerForm.confirmPassword"
            type="password"
            required
            autocomplete="new-password"
            placeholder="Повторите пароль"
            class="w-full bg-transparent border-0 border-b py-2.5 text-ink placeholder:text-ink-muted/50 focus:outline-none transition-colors"
            :class="registerForm.confirmPassword && !passwordsMatch
              ? 'border-accent'
              : 'border-border-hover focus:border-ink'
            "
          />
          <p
            v-if="registerForm.confirmPassword && !passwordsMatch"
            class="text-xs text-accent mt-1"
          >
            Пароли не совпадают
          </p>
        </div>

        <button
          type="submit"
          :disabled="isLoading || (!!registerForm.confirmPassword && !passwordsMatch)"
          class="w-full py-3 bg-ink text-white text-sm font-medium hover:bg-ink/90 transition-colors disabled:opacity-50"
        >
          {{ isLoading ? 'Регистрация...' : 'Создать аккаунт' }}
        </button>

        <p class="text-center text-xs text-ink-muted">
          Уже есть аккаунт?
          <button
            type="button"
            class="text-ink underline underline-offset-2 hover:text-accent transition-colors"
            @click="switchTab('login')"
          >
            Войдите
          </button>
        </p>
      </form>

      <!-- Decorative quote -->
      <div class="mt-12 pt-8 border-t border-border text-center">
        <blockquote class="text-xs text-ink-muted italic leading-relaxed">
          «Наш долг — сохранить память о каждом, чьё имя пытались стереть»
        </blockquote>
      </div>
    </div>
  </div>
</template>
