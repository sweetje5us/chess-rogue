# Настройка окружения для Chess Rogue Android

## Проблема с модулем "app"

Если в Android Studio показывается только `<no module>`, это означает проблемы с Gradle синхронизацией.

## Решение 1: Установка Java 8-11

1. **Скачайте Java 8 или 11:**
   - [Oracle JDK 8](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)
   - Или [OpenJDK 11](https://adoptium.net/)

2. **Установите Java** и добавьте в PATH

3. **Проверьте установку:**
   ```bash
   java -version
   ```

## Решение 2: Настройка Android Studio

1. **Откройте Android Studio**
2. **Выберите "Open an existing project"**
3. **Найдите папку `android` в проекте**
4. **Дождитесь синхронизации Gradle** (может занять несколько минут)

## Решение 3: Ручная настройка конфигурации

1. В Android Studio перейдите в `File → Project Structure`
2. В разделе `Modules` нажмите `+` → `Import Module`
3. Выберите папку `android/app`
4. Следуйте инструкциям мастера

## Решение 4: Альтернативная сборка

Если Android Studio не работает, используйте командную строку:

```bash
# Установите Android SDK Command Line Tools
# Добавьте в PATH: ANDROID_HOME/tools и ANDROID_HOME/platform-tools

# Затем запустите:
.\build-apk.bat
```

## Требования для сборки

- **Java 8-11** (не Java 19+)
- **Android SDK** (API 22+)
- **Android Studio** (рекомендуется) или Command Line Tools
- **Gradle** (уже включен в проект)

## Проверка окружения

```bash
# Проверка Java
java -version

# Проверка Android SDK (если установлен)
adb version

# Проверка Gradle
cd android
.\gradlew --version
```

## Если ничего не помогает

1. Удалите папку `.gradle` в папке `android`
2. Перезапустите Android Studio
3. Выполните `npx cap sync android`
4. Откройте проект заново 