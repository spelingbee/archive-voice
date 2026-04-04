# 🕊️ Голос из архива / Voice from the Archive

> Цифровой исторический архив жертв политических репрессий в Кыргызстане.
> Платформа для поиска, документирования и сохранения памяти о репрессированных — с ИИ-ассистентом на основе RAG.

![Nuxt 4](https://img.shields.io/badge/Nuxt-4.3-00DC82?logo=nuxt.js&logoColor=white)
![Vue 3](https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vue.js&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4.2-06B6D4?logo=tailwindcss&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178C6?logo=typescript&logoColor=white)

---

## 📖 О проекте

**«Голос из архива»** — это веб-платформа, которая оцифровывает и делает доступными для исследователей, историков и широкой общественности данные о жертвах советских политических репрессий на территории Кыргызстана (1920–1950-е гг.).

### Проблема

Тысячи архивных записей о репрессированных гражданах остаются разрозненными, труднодоступными и не систематизированными. Семьи не могут найти информацию о своих предках, а исследователи тратят месяцы на работу с бумажными архивами.

### Решение

Платформа предоставляет:

- **📋 Цифровой архив** — структурированная база данных репрессированных с поиском, фильтрацией и верификацией записей
- **🤖 ИИ-ассистент (RAG)** — чат-бот, который отвечает на вопросы по архивным документам, цитируя источники, в режиме реального времени (SSE стриминг)
- **🔐 Ролевая система** — разделение прав для исследователей, модераторов и администраторов, позволяющее верифицировать записи
- **🌐 Открытость** — любой человек может искать по архиву и предложить дополнительную информацию

### Целевая аудитория

| Пользователь | Сценарий |
|---|---|
| 👨‍👩‍👧 Потомки репрессированных | Найти информацию о родственниках |
| 🎓 Историки и исследователи | Систематический анализ архива |
| 📰 Журналисты | Работа с историческими материалами |
| 🏛️ Государственные органы | Верификация и реабилитация |

---

## 🚀 Быстрый старт

### Предварительные требования

| Инструмент | Версия | Назначение |
|---|---|---|
| Node.js | ≥ 20.x | Среда выполнения фронтенда |
| pnpm | ≥ 9.x | Пакетный менеджер |
| Java | 21 | Среда выполнения бэкенда |
| PostgreSQL | ≥ 15 | Основная база данных |

### Установка и запуск

#### 1. Клонирование репозитория

```bash
git clone https://github.com/<your-org>/archive-voice.git
cd archive-voice
```

#### 2. Фронтенд

```bash
cd frontend

# Установить зависимости
pnpm install

# Запустить dev-сервер (http://localhost:3000)
pnpm dev
```

#### 3. Бэкенд

```bash
cd backend

# Запустить Spring Boot (http://localhost:8080)
./gradlew bootRun
```

#### 4. Переменные окружения (фронтенд)

Создать файл `frontend/.env`:

```env
# URL Spring Boot API
NUXT_PUBLIC_API_BASE=http://localhost:8080/api
```

### Сборка для production

```bash
cd frontend
pnpm build        # Сборка в .output/
pnpm preview      # Предварительный просмотр сборки
```

---

## 🛠️ Технологический стек

### Frontend

| Технология | Версия | Назначение |
|---|---|---|
| **Nuxt** | 4.3 | Фреймворк (SSR/SSG, маршрутизация, авто-импорты) |
| **Vue** | 3.5 | Composition API, реактивность |
| **TypeScript** | 6.0 | Статическая типизация |
| **Tailwind CSS** | 4.2 | Утилитарные стили (CSS-first конфигурация) |
| **@nuxt/fonts** | 0.14 | Автоподключение шрифтов (Playfair Display, Inter, JetBrains Mono) |
| **ofetch** | встроен | HTTP-клиент (встроен в Nuxt) |

### Backend

| Технология | Версия | Назначение |
|---|---|---|
| **Spring Boot** | 3.4 | REST API framework |
| **Spring Security** | 6.x | JWT-аутентификация, RBAC |
| **Spring Data JPA** | 3.x | ORM (Hibernate) |
| **PostgreSQL** | 15+ | Реляционная БД |
| **Lombok** | — | Генерация boilerplate-кода |

---

# 📐 Для жюри: Архитектура и дизайн-решения

> Этот раздел объясняет **почему** проект устроен именно так, какие компромиссы были приняты и какой архитектурный подход используется.

## Архитектурный подход: Mini-FSD (Feature-Sliced Design) для Nuxt

### Проблема, которую мы решали

На старте проект представлял собой **монолитный `index.vue`** (~250 строк) с:
- Захардкоженными данными (карточки репрессированных прямо в шаблоне)
- Inline-типами (`interface Person {}` внутри `<script>`)
- Отсутствием API-слоя
- Единым компонентом чата без реального стриминга

Это типичная ситуация для MVP, но при масштабировании (больше доменов, ролей, интеграций) такой подход превращается в «большой ком грязи».

### Почему Mini-FSD, а не полный FSD?

Мы выбрали **адаптированный Feature-Sliced Design**, потому что:

| Критерий | Полный FSD | Наш Mini-FSD | Почему |
|---|---|---|---|
| Слои | 7 слоёв (app, processes, pages, widgets, features, entities, shared) | 3 уровня (shared → features → pages) | Nuxt берёт на себя роль `app` и `pages` слоёв, а разница между `entities` и `features` на данном этапе — оверинжиниринг |
| Сегменты | ui, model, api, lib, config | types, api, composables, components | Адаптировано под Vue/Nuxt экосистему |
| Public API | Строгий `index.ts` в каждом слайсе | Прямые импорты + авто-импорты Nuxt | Nuxt auto-imports делает barrel-файлы избыточными для composables |

### Принцип: «Не ломай Nuxt»

Nuxt имеет жёсткие конвенции (`pages/`, `components/`, `composables/`). Вместо борьбы с фреймворком мы **расширяем** его:

```
app/
├── composables/          ← Nuxt auto-import (глобальный)
├── components/layout/    ← Nuxt auto-import (глобальный)
├── pages/                ← Nuxt file-based routing
└── features/             ← 🆕 Наш доменный слой
    ├── archive/          ← Домен «Архив» (types, api, composables, components)
    ├── chat/             ← Домен «Чат»
    └── auth/             ← Домен «Авторизация»
```

Composables и компоненты из `features/` тоже авто-импортируются — это настроено в `nuxt.config.ts`:

```ts
imports: { dirs: ["features/*/composables"] },
components: [
  { path: "~/features/archive/components", pathPrefix: false },
  // ...
]
```

## Структура файлов проекта

```
frontend/app/
│
├── app.vue                              # Точка входа: <NuxtPage/>
│
├── types/                               # Общие типы (Shared layer)
│   ├── api.ts                           # ApiResponse<T>, PaginatedResponse<T>, ApiError
│   ├── auth.ts                          # AuthUser, UserRole, LoginCredentials
│   └── index.ts                         # Barrel re-export
│
├── composables/                         # Глобальные composables (Shared layer)
│   └── useApiFetch.ts                   # API-клиент с JWT + обработка 401
│
├── features/                            # Доменный слой (Feature layer)
│   │
│   ├── archive/                         # 📂 Домен: База репрессированных
│   │   ├── types.ts                     # Person, PersonId (branded), VerificationStatus
│   │   ├── api.ts                       # archiveRepository: getList, getById, create, verify
│   │   ├── composables/
│   │   │   └── useArchivePerson.ts      # Бизнес-логика: canVerify, canEdit, handleVerify
│   │   └── components/
│   │       └── PersonCard.vue           # Карточка с ролевой кнопкой верификации
│   │
│   ├── chat/                            # 📂 Домен: RAG ИИ-ассистент
│   │   ├── types.ts                     # ChatMessage, StreamStatus
│   │   ├── composables/
│   │   │   └── useChatStream.ts         # SSE-стриминг через ReadableStream
│   │   └── components/
│   │       └── ChatSlideOver.vue        # Slide-over панель чата с typewriter-эффектом
│   │
│   └── auth/                            # 📂 Домен: Авторизация
│       └── composables/
│           └── useAuth.ts               # login/logout, роли (isAdmin, isModerator)
│
├── components/
│   └── layout/                          # Layout-компоненты
│       ├── AppHeader.vue                # Навигация с auth-aware UI
│       └── AppFooter.vue                # Футер
│
└── pages/
    └── index.vue                        # Оркестратор (~90 строк вместо ~250)
```

## Ключевые дизайн-решения

### 1. API-слой: два клиента вместо одного

```
useApiFetch<T>()  — для GET (SSR, кеширование, реактивность)
$apiFetch<T>()    — для мутаций POST/PUT/DELETE (императивный)
```

**Почему:** Nuxt `useFetch` оптимизирован для SSR-гидратации и дедупликации запросов, но не подходит для мутаций (POST, PUT, DELETE). Вместо одного универсального клиента мы предоставляем два специализированных — каждый с JWT-интерцептором и обработкой 401.

### 2. SSE-стриминг: нативный fetch вместо $fetch

```ts
// ❌ $fetch / useFetch — буферизирует ответ целиком
const data = await $fetch('/api/chat')

// ✅ native fetch + ReadableStream — читаем по чанкам
const response = await fetch('/api/chat', { method: 'POST' })
const reader = response.body.pipeThrough(new TextDecoderStream()).getReader()
```

**Почему:** `ofetch` (на котором построен `$fetch` в Nuxt) не поддерживает стриминг — он ждёт полного ответа. Для SSE (Server-Sent Events) с typewriter-эффектом необходим доступ к `ReadableStream`, который есть только в нативном `fetch`.

### 3. Branded types для ID

```ts
type PersonId = number & { readonly __brand: 'PersonId' }
```

**Почему:** Без branded types ничто не мешает передать `userId` туда, где ожидается `personId` — оба `number`. Branded types создают номинальную типизацию в структурной системе TypeScript.

### 4. Repository Pattern для API

```ts
archiveRepository.getList(filters)    // не useApiFetch('/archive/persons')
archiveRepository.verify(id)          // не $apiFetch('/archive/persons/1/verify')
```

**Почему:** Компоненты и composables не должны знать URL'ы API. Репозиторий инкапсулирует HTTP-детали, а при смене эндпоинта на бэкенде правится один файл.

### 5. Роли через Composable, а не директивы

```ts
const { canVerify, canEdit } = useArchivePerson(id)
// в шаблоне: v-if="canVerify"
```

**Почему:** Бизнес-правило «верифицировать может только ADMIN/MODERATOR и только pending-записи» — это **доменная логика**, а не UI-логика. Она живёт в composable, а компонент только спрашивает «можно ли?».

### 6. Дизайн: «Архивная эстетика» (Academic Archive)

Визуальная концепция вдохновлена бумажными архивами и академическими изданиями:

| Решение | Значение | Обоснование |
|---|---|---|
| `--color-paper: #fcfcfa` | Фон | Имитация пожелтевшей бумаги |
| `--color-ink: #111827` | Текст | Чернильный цвет, не чистый чёрный |
| `--color-accent: #8b0000` | Акцент | Тёмно-красный — ассоциация с историческими документами и печатями |
| Playfair Display | Заголовки | Каллиграфический шрифт с засечками — как в старых книгах |
| Inter | Основной текст | Нейтральный и разборчивый для длинных текстов |
| JetBrains Mono | Даты, метаданные | Моноширинный — как на печатной машинке |
| Минимум цвета | Общий стиль | Уважение к теме: это не развлекательная платформа, а мемориальный проект |

Это **осознанное решение**: вместо ярких градиентов и анимаций мы выбрали сдержанный, документальный стиль, соответствующий серьёзности темы.

## Граф зависимостей

```
┌──────────────────────────────────────────────────────┐
│                     pages/                           │
│                   index.vue                          │
│              (тонкий оркестратор)                     │
└────────────┬──────────────┬──────────────┬────────────┘
             │              │              │
     ┌───────▼──────┐ ┌────▼─────┐ ┌──────▼───────┐
     │   features/  │ │ features/│ │  features/   │
     │   archive/   │ │  chat/   │ │    auth/     │
     │              │ │          │ │              │
     │ PersonCard   │ │ ChatSlide│ │  useAuth()   │
     │ useArchive   │ │ useChatSt│ │              │
     │ archiveRepo  │ │ ream()   │ │              │
     └──────┬───────┘ └────┬─────┘ └──────┬───────┘
            │              │              │
     ┌──────▼──────────────▼──────────────▼───────┐
     │              composables/                   │
     │  useApiFetch.ts  /  $apiFetch               │
     └────────────────────┬────────────────────────┘
                          │
     ┌────────────────────▼────────────────────────┐
     │                 types/                       │
     │    api.ts  ·  auth.ts  (shared contracts)    │
     └─────────────────────────────────────────────┘
```

**Зависимости направлены строго вниз:** страницы → фичи → shared. Фичи не импортируют друг друга напрямую (кроме `auth`, которая является cross-cutting concern через composable `useAuth()`).

## Что будет дальше (Roadmap)

- [ ] **Route middleware** — защита маршрутов (`/admin`, `/archive/new`) через `defineNuxtRouteMiddleware`
- [ ] **Страница `/archive/:id`** — детальная карточка репрессированного с документами
- [ ] **Страница `/auth`** — формы входа и регистрации
- [ ] **Пагинация** — бесконечный скролл или классическая пагинация
- [ ] **i18n** — кыргызский, русский, английский языки
- [ ] **Тёмная тема** — переключение для пользователя (CSS custom properties уже заложены)
- [ ] **PWA** — офлайн-доступ к кешированным записям

---

## 📄 Лицензия

Проект создан в образовательных и исследовательских целях в рамках сохранения исторической памяти Кыргызстана.

---

<p align="center">
  <em>«Кто не помнит прошлого — обречён повторить его»</em><br>
  <sub>Джордж Сантаяна</sub>
</p>
