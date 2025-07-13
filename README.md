# Chess Rogue - Android Build

## Запуск в Android Studio

### 1. Открытие проекта
1. Запустите Android Studio
2. Выберите "Open an existing project"
3. Найдите папку `android` в корне проекта и откройте её
4. Дождитесь синхронизации Gradle

### 2. Настройка конфигурации
1. В Android Studio перейдите в `Run → Edit Configurations`
2. Нажмите `+` и выберите `Android App`
3. Настройте конфигурацию:
   - **Name**: `Chess Rogue`
   - **Module**: `app`
   - **Launch Options**: `Default Activity`
4. Нажмите `OK`

### 3. Запуск на устройстве/эмуляторе
1. Подключите Android-устройство через USB (включите режим разработчика)
2. Или создайте эмулятор: `Tools → AVD Manager → Create Virtual Device`
3. Нажмите зеленую кнопку "Run" (▶️) или `Shift + F10`

### 4. Сборка APK
1. Выберите `Build → Build APK(s)`
2. APK будет создан в: `android/app/build/outputs/apk/debug/app-debug.apk`

## Команды для разработки

```bash
# Синхронизация изменений
npx cap sync android

# Открытие в Android Studio
npx cap open android

# Запуск на устройстве
npx cap run android
```

## Требования
- Android Studio 4.0+
- Android SDK API 22+
- Java 8+ 