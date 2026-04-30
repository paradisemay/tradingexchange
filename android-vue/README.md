# Android Vue Trading Terminal

Клиентское приложение Android Vue для биржевого терминала по Mini-SRS.

## Unified stack note

Основной запуск всей платформы теперь выполняется из корня репозитория:

```bash
cd ..
docker compose up --build -d
```

APK-сборка через тот же root compose:

```bash
docker compose --profile apk run --rm android-vue-apk
```

## Стек

- Vue 3 Composition API + TypeScript
- Ionic Vue
- Pinia
- VeeValidate + Zod
- Axios + JWT refresh очередь
- WebSocket quotes с reconnect + resubscribe
- @capacitor/preferences для токенов
- IndexedDB для офлайн-кэша портфеля/инструментов/котировок

## Требования

- Node.js 20+
- npm 10+
- Для Android-сборки: Android Studio + Android SDK + JDK 17

## Установка

```bash
npm install
```

## Переменные окружения

Создай `.env` из примера:

```bash
cp .env.example .env
```

Пример:

```env
VITE_API_BASE_URL=http://localhost:8080
```

`VITE_API_BASE_URL` должен указывать на Ktor (или mock server API-модуля).

## Локальный запуск (dev)

```bash
npm run dev
```

Приложение откроется на адресе Vite (обычно `http://localhost:5173`).

## Доступные команды

- `npm run dev` - запуск dev-сервера
- `npm run build` - проверка TypeScript + production сборка
- `npm run preview` - локальный просмотр production build
- `npm run cap:sync` - синхронизация web-части в Android-проект Capacitor
- `npm run cap:open` - открыть нативный Android проект в Android Studio
- `npm run android:build` - `build + cap:sync`

## Сборка Android (Capacitor)

Первичный шаг (один раз, если платформа еще не добавлена):

```bash
npx cap add android
```

Далее:

```bash
npm run android:build
npm run cap:open
```

После `cap:open`:

- в Android Studio выбрать эмулятор/устройство и нажать Run,
- либо собрать APK через Build menu.

## Проверка интеграции с Ktor

Перед UI-проверкой убедись, что Ktor поднят и доступен по `VITE_API_BASE_URL`.

Минимальный API smoke:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/me`
- `GET /api/v1/portfolio`
- `GET /api/v1/instruments?query=SBER`
- `POST /api/v1/orders`
- `GET /api/v1/orders`
- `GET /api/v1/transactions`
- `GET /api/v1/quotes/ws` (WebSocket handshake)

## Тестирование модуля

Автотесты в модуле не добавлены; базовая проверка выполняется через сборку и ручной e2e сценарий.

### 1) Проверка сборки

```bash
npm run build
```

Ожидаемо: успешный TypeScript-check и сборка Vite.

### 2) Ручной e2e сценарий

Прогонить последовательно:

1. Регистрация
2. Логин
3. Портфель
4. Поиск инструмента
5. Покупка (`BUY`)
6. Продажа (`SELL`)
7. История ордеров
8. История транзакций
9. Logout

### 3) Отдельные проверки

- **Refresh flow:** при истечении access token запросы должны пройти через единичный refresh и повтор.
- **Offline cache:** при отключении сети должны отображаться кэшированные портфель/инструменты и индикатор offline.
- **WebSocket:** live-котировки обновляют портфель; после обрыва соединения происходит reconnect + повторная подписка.
- **Валидация заявок:** для `MARKET` `limitPrice=null`, для `LIMIT` `limitPrice` обязателен.

## Реализованные экраны (v1)

- Login / Register / Logout
- Profile (`GET /api/v1/me`)
- Portfolio (`GET /api/v1/portfolio`) + live quotes
- Instruments search (`GET /api/v1/instruments`)
- Create order (`POST /api/v1/orders`) с валидацией `MARKET/LIMIT`
- Orders history (`GET /api/v1/orders`)
- Transactions history (`GET /api/v1/transactions`)

## Структура модуля

`src/api/` - REST клиенты Axios  
`src/stores/` - Pinia stores  
`src/composables/` - переиспользуемая логика (ws, refresh, offline)  
`src/views/` - экраны  
`src/components/` - UI компоненты  
`src/router/` - маршрутизация  
`src/utils/` - форматирование, errorCode маппинг, storage helper

