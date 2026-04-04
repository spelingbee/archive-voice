<script setup lang="ts">
/**
 * pages/auth.vue
 * Страница авторизации — вход и регистрация.
 *
 * Использует useAuth() для login/register.
 * При успехе → редирект на / (или на предыдущую страницу).
 */

const { t } = useI18n()

// --- SEO ---
useHead({ title: `${t('auth.login_tab')} — ${t('header.title')}` })

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
  firstName: '',
  lastName: '',
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
  } catch (err: any) {
    errorMessage.value = err.response?._data?.message || t('auth.login_error')
  } finally {
    isLoading.value = false
  }
}

async function handleRegister() {
  errorMessage.value = null

  if (!passwordsMatch.value) {
    errorMessage.value = t('auth.passwords_dont_match')
    return
  }

  isLoading.value = true

  try {
    await register({
      firstName: registerForm.value.firstName,
      lastName: registerForm.value.lastName,
      email: registerForm.value.email,
      password: registerForm.value.password,
    })
  } catch (err: any) {
    errorMessage.value = err.response?._data?.message || t('auth.register_error')
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
          {{ activeTab === 'login' ? t('auth.login_title') : t('auth.register_title') }}
        </h2>
        <p class="text-sm text-ink-muted">
          {{ activeTab === 'login'
            ? t('auth.login_desc')
            : t('auth.register_desc')
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
          {{ t('auth.login_tab') }}
        </button>
        <button
          class="flex-1 pb-3 text-sm font-medium transition-colors border-b-2 -mb-px"
          :class="activeTab === 'register'
            ? 'border-ink text-ink'
            : 'border-transparent text-ink-muted hover:text-ink'
          "
          @click="switchTab('register')"
        >
          {{ t('auth.register_tab') }}
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
            {{ t('auth.email') }}
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
            {{ t('auth.password') }}
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
          {{ isLoading ? t('auth.logging_in') : t('auth.login_tab') }}
        </button>

        <p class="text-center text-xs text-ink-muted">
          {{ t('auth.no_account') }}
          <button
            type="button"
            class="text-ink underline underline-offset-2 hover:text-accent transition-colors"
            @click="switchTab('register')"
          >
            {{ t('auth.register_now') }}
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
          <label for="reg-first-name" class="block text-sm text-ink-secondary mb-2">
            {{ t('auth.first_name') }}
          </label>
          <input
            id="reg-first-name"
            v-model="registerForm.firstName"
            type="text"
            required
            autocomplete="given-name"
            :placeholder="t('auth.first_name') + '...'"
            class="w-full bg-transparent border-0 border-b border-border-hover py-2.5 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
          />
        </div>

        <div>
          <label for="reg-last-name" class="block text-sm text-ink-secondary mb-2">
            {{ t('auth.last_name') }}
          </label>
          <input
            id="reg-last-name"
            v-model="registerForm.lastName"
            type="text"
            required
            autocomplete="family-name"
            :placeholder="t('auth.last_name') + '...'"
            class="w-full bg-transparent border-0 border-b border-border-hover py-2.5 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
          />
        </div>

        <div>
          <label for="reg-email" class="block text-sm text-ink-secondary mb-2">
            {{ t('auth.email') }}
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
            {{ t('auth.password') }}
          </label>
          <input
            id="reg-password"
            v-model="registerForm.password"
            type="password"
            required
            autocomplete="new-password"
            minlength="8"
            :placeholder="t('auth.password') + '...'"
            class="w-full bg-transparent border-0 border-b border-border-hover py-2.5 text-ink placeholder:text-ink-muted/50 focus:border-ink focus:outline-none transition-colors"
          />
        </div>

        <div>
          <label for="reg-confirm" class="block text-sm text-ink-secondary mb-2">
            {{ t('auth.confirm_password') }}
          </label>
          <input
            id="reg-confirm"
            v-model="registerForm.confirmPassword"
            type="password"
            required
            autocomplete="new-password"
            :placeholder="t('auth.confirm_password') + '...'"
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
            {{ t('auth.passwords_dont_match') }}
          </p>
        </div>

        <button
          type="submit"
          :disabled="isLoading || (!!registerForm.confirmPassword && !passwordsMatch)"
          class="w-full py-3 bg-ink text-white text-sm font-medium hover:bg-ink/90 transition-colors disabled:opacity-50"
        >
          {{ isLoading ? t('auth.registering') : t('auth.create_account') }}
        </button>

        <p class="text-center text-xs text-ink-muted">
          {{ t('auth.have_account') }}
          <button
            type="button"
            class="text-ink underline underline-offset-2 hover:text-accent transition-colors"
            @click="switchTab('login')"
          >
            {{ t('auth.login_now') }}
          </button>
        </p>
      </form>

      <!-- Decorative quote -->
      <div class="mt-12 pt-8 border-t border-border text-center">
        <blockquote class="text-xs text-ink-muted italic leading-relaxed">
          {{ t('auth.quote') }}
        </blockquote>
      </div>
    </div>
  </div>
</template>
