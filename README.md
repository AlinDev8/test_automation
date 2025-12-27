# Проект автотестирования (Web + Mobile)

## Описание проекта
Данный репозиторий содержит учебный проект по автоматизации тестирования:

- **Web-автотесты** для сайта (UI-тестирование в браузере);
- **Mobile-автотесты** для мобильного приложения **Wikipedia** (Android).

Проект выполнен в рамках курса по автоматизации тестирования и предназначен для демонстрации навыков работы с:

- Java
- Selenium / Selenide (web)
- Appium (mobile)
- Maven
- Page Object Model

Репозиторий подготовлен для проверки ментором и последующего добавления в портфолио.

---

## Структура проекта

```
test_automation
├─.idea
├─src
|   ├─main/java/ru/javabruse
|   |  ├─pages
|   |  |  ├─WikipediaAppPage.java
|   |  |  └─WikipediaPage.java
|   |  └─utils
|   |     └─WebDriverFactory.java
|   └─test/java/ru/javabruse
|      ├─mobile
|      |  └─WikipediaMobileTests.java
|      └─web
|         └─WikipediaTests.java
├─pom.xml
└─README
```

---

## Используемые технологии и библиотеки

- **Java 11+**
- **Maven** — управление зависимостями
- **JUnit 5** — тестовый фреймворк
- **Selenium / Selenide** — автоматизация веб-тестов
- **Appium** — автоматизация мобильных тестов
- **Android Emulator** (Android Studio)

Все зависимости подключены через `pom.xml`.

---

## Требования к окружению

Перед запуском тестов убедитесь, что установлены:

- JDK 11 или выше
- Maven 3.8+
- Google Chrome (или другой поддерживаемый браузер)
- Android Studio
- Android SDK
- Создан и запущен Android-эмулятор
- Appium Server (Desktop или CLI)

Проверьте, что переменные окружения `JAVA_HOME` и `ANDROID_HOME` настроены корректно.

---

## Запуск Web-тестов

1. Клонировать репозиторий:

```bash
git clone https://github.com/AlinDev8/test_automation
cd SimpleUiTest
```

2. Запустить web-тесты командой Maven (Selenium):

```bash
mvn test -Dgroups=web
```

3. После выполнения тестов результаты будут доступны в консоли и отчётах Maven.

### Проверяемые web-сценарии

- Главная страница (англ)
- Поиск статьи “Selenium (software)”
- Проверка блока Featured article
- Открытие случайной статьи.

---

## Запуск Mobile-тестов (Wikipedia, Appium, Android)

### Предварительные шаги

1. Запустить Android Emulator через Android Studio.
2. Убедится что Android SDK установлен
Переменные окружения:
```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator"
```
3. Убедиться, что Appium установлен и драйвер UiAtomator2.
```bash
npm install -g appium
appium driver install uiautomator2
```
4. Проверить, что Appium сервер запущен.
5. Запустить Android Emulator через Android Studio.

### Запуск тестов

```bash
mvn test -Dgroups=mobile \
  -DappiumServerUrl=http://127.0.0.1:4723/wd/hub \
  -DdeviceName="emulator-5554" \
  -DplatformVersion=16
```

### Проверяемые мобильные сценарии

- Поиск “Selenium” с проверкой результатов и описаний
- Открытие статьи из поиска
- Проверка заголовка статьи
- Очистка запроса и проверка

---
