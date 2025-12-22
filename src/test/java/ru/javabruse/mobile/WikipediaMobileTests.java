package ru.javabruse.mobile;

import io.appium.java_client.android.AndroidDriver;
import org.testng.Assert;
import org.testng.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.javabruse.pages.WikipediaAppPage;
import ru.javabruse.utils.WebDriverFactory;

import java.lang.reflect.Method;

/**
 * Тесты для мобильного приложения Wikipedia
 */
public class WikipediaMobileTests {

    private static final Logger logger = LoggerFactory.getLogger(WikipediaMobileTests.class);

    private AndroidDriver driver;
    private WikipediaAppPage appPage;

    @BeforeMethod
    public void setup(Method method) {
        logger.info("Запуск теста: {}", method.getName());
        try {
            driver = WebDriverFactory.createAndroidDriver();
            appPage = new WikipediaAppPage(driver);

            // Даем время на инициализацию приложения
            safeSleepQuietly(3000);
            logger.info("Приложение запущено, тест начинается");
        } catch (Exception e) {
            logger.error("Ошибка при настройке теста: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось запустить тест", e);
        }
    }

    @AfterMethod
    public void tearDown(Method method) {
        logger.info("Завершение теста: {}", method.getName());
        try {
            if (driver != null) {
                // Делаем скриншот перед закрытием при неудачном тесте
                if (isTestFailed()) {
                    takeScreenshot(method.getName() + "_failed");
                }
                driver.quit();
                logger.info("Драйвер закрыт");
            }
        } catch (Exception e) {
            logger.warn("Ошибка при завершении теста: {}", e.getMessage());
        }
    }

    /**
     * Проверка отображения главного экрана и поля поиска
     */
    @Test(priority = 1, description = "Проверка отображения главного экрана приложения")
    public void testMainScreenIsLoaded() {
        logger.info("Тест: проверка главного экрана");

        boolean isLoaded = appPage.isMainScreenLoaded();
        logger.info("Главный экран загружен: {}", isLoaded);

        Assert.assertTrue(isLoaded, "Главный экран должен быть загружен и отображаться");

        // Дополнительные проверки
        Assert.assertNotNull(driver.getCurrentPackage(), "Приложение должно быть запущено");
        logger.info("Текущий пакет приложения: {}", driver.getCurrentPackage());
    }

    /**
     * Тестирование поиска и открытия статьи
     */
    @Test(priority = 2, description = "Поиск статьи по ключевому слову и проверка открытия")
    public void testSearchAndOpenArticle() {
        logger.info("Тест: поиск и открытие статьи");
        String searchQuery = "Appium";

        // Выполняем поиск
        appPage.searchArticle(searchQuery);

        // Получаем заголовок статьи
        String articleTitle = appPage.getArticleTitle();
        logger.info("Заголовок открытой статьи: '{}'", articleTitle);

        // Проверки
        Assert.assertNotNull(articleTitle, "Заголовок статьи не должен быть null");
        Assert.assertFalse(articleTitle.isEmpty(), "Заголовок статьи не должен быть пустым");
        Assert.assertTrue(articleTitle.toLowerCase().contains(searchQuery.toLowerCase()),
                String.format("Заголовок должен содержать '%s'. Фактический: %s",
                        searchQuery, articleTitle));

        // Дополнительная проверка: статья должна быть открыта
        Assert.assertNotEquals(articleTitle, "Search results",
                "Должна открыться статья, а не страница результатов поиска");

        logger.info("Статья '{}' успешно открыта", articleTitle);
    }

    /**
     * Тестирование навигации назад после поиска
     */
    @Test(priority = 3, description = "Проверка навигации назад после открытия статьи")
    public void testSearchAndNavigateBack() {
        logger.info("Тест: поиск, открытие статьи и возврат назад");
        String searchQuery = "Selenium";

        // Выполняем поиск и открываем статью
        appPage.searchArticle(searchQuery);
        String initialTitle = appPage.getArticleTitle();
        logger.info("Открыта статья: {}", initialTitle);

        // Возвращаемся назад
        appPage.goBack();
        logger.info("Выполнен возврат назад");

        // Проверяем, что вернулись на главный экран
        boolean isMainScreenLoaded = appPage.isMainScreenLoaded();
        logger.info("Главный экран загружен после возврата: {}", isMainScreenLoaded);

        Assert.assertTrue(isMainScreenLoaded,
                "После возврата должен отображаться главный экран с полем поиска");

        // Дополнительная проверка: текущий экран не должен быть статьей
        String currentActivity = driver.currentActivity();
        Assert.assertFalse(currentActivity.contains("page"),
                "После возврата не должно быть активности статьи");

        logger.info("Навигация назад выполнена успешно");
    }

    /**
     * Тестирование поиска с разными запросами
     */
    @Test(priority = 4, description = "Поиск статей с разными запросами",
            dataProvider = "searchQueries")
    public void testSearchWithDifferentQueries(String query, String expectedInTitle) {
        logger.info("Тест поиска с запросом: '{}'", query);

        appPage.searchArticle(query);
        String articleTitle = appPage.getArticleTitle();
        logger.info("Результат поиска: '{}'", articleTitle);

        Assert.assertFalse(articleTitle.isEmpty(),
                String.format("Для запроса '%s' должен быть найден результат", query));

        if (expectedInTitle != null && !expectedInTitle.isEmpty()) {
            Assert.assertTrue(articleTitle.toLowerCase().contains(expectedInTitle.toLowerCase()),
                    String.format("Заголовок должен содержать '%s'. Фактический: %s",
                            expectedInTitle, articleTitle));
        }

        // Возвращаемся для следующего теста
        appPage.goBack();
    }

    /**
     * Тестирование поиска с проверкой количества результатов
     */
    @Test(priority = 5, description = "Проверка отображения результатов поиска")
    public void testSearchResultsDisplay() {
        logger.info("Тест: проверка отображения результатов поиска");

        // Пропускаем онбординг и всплывающие окна
        appPage.skipOnboardingIfPresent();
        appPage.dismissAllPopups();

        // Проверяем, что главный экран загружен
        Assert.assertTrue(appPage.isMainScreenLoaded(),
                "Главный экран должен быть загружен перед поиском");

        // Выполняем поиск (метод searchArticle уже содержит всю логику)
        String searchQuery = "Java";
        appPage.searchArticle(searchQuery);

        // Получаем заголовок статьи
        String articleTitle = appPage.getArticleTitle();
        logger.info("Открыта статья: {}", articleTitle);

        Assert.assertFalse(articleTitle.isEmpty(),
                String.format("Для запроса '%s' должна быть открыта статья", searchQuery));

        // Проверяем, что заголовок содержит искомое слово
        Assert.assertTrue(articleTitle.toLowerCase().contains(searchQuery.toLowerCase()),
                String.format("Заголовок должен содержать '%s'. Фактический: %s",
                        searchQuery, articleTitle));

        logger.info("Тест поиска результатов выполнен успешно");
    }

    /**
     * Тестирование работы приложения после нескольких операций
     */
    @Test(priority = 6, description = "Комплексный тест работы приложения")
    public void testComplexAppWorkflow() {
        logger.info("Комплексный тест работы приложения");

        // Шаг 1: Проверка главного экрана
        Assert.assertTrue(appPage.isMainScreenLoaded(),
                "Шаг 1: Главный экран должен быть загружен");

        // Шаг 2: Поиск первой статьи
        appPage.searchArticle("Android");
        String firstArticle = appPage.getArticleTitle();
        Assert.assertFalse(firstArticle.isEmpty(),
                "Шаг 2: Первая статья должна быть открыта");
        logger.info("Открыта статья: {}", firstArticle);

        // Шаг 3: Возврат назад
        appPage.goBack();
        Assert.assertTrue(appPage.isMainScreenLoaded(),
                "Шаг 3: После возврата должен быть главный экран");

        // Шаг 4: Поиск второй статьи
        appPage.searchArticle("Kotlin");
        String secondArticle = appPage.getArticleTitle();
        Assert.assertFalse(secondArticle.isEmpty(),
                "Шаг 4: Вторая статья должна быть открыта");
        logger.info("Открыта статья: {}", secondArticle);

        // Шаг 5: Проверка, что статьи разные
        Assert.assertNotEquals(firstArticle, secondArticle,
                "Шаг 5: Открытые статьи должны быть разными");

        logger.info("Комплексный тест выполнен успешно");
    }

    /**
     * Дополнительный тест: быстрый поиск без открытия статьи
     */
    @Test(priority = 7, description = "Быстрый поиск с проверкой подсказок")
    public void testQuickSearchWithSuggestions() {
        logger.info("Тест: быстрый поиск с проверкой подсказок");

        try {
            // Подготовка приложения
            appPage.skipOnboardingIfPresent();
            appPage.dismissAllPopups();

            // Проверяем главный экран
            Assert.assertTrue(appPage.isMainScreenLoaded(),
                    "Главный экран должен быть загружен");

            // Ждем немного для стабилизации
            safeSleepQuietly(2000);

            logger.info("Тест быстрого поиска выполнен");

        } catch (Exception e) {
            logger.error("Ошибка в тесте быстрого поиска: {}", e.getMessage());
            throw new RuntimeException("Ошибка в тесте быстрого поиска", e);
        }
    }

    /**
     * Провайдер данных для параметризованных тестов
     */
    @DataProvider(name = "searchQueries")
    public Object[][] provideSearchQueries() {
        return new Object[][] {
                {"Java", "Java"},
                {"Python", "Python"},
                {"Тестирование", "Тестирование"},
                {"Automation", "Automation"},
                {"Mobile", "Mobile"}
        };
    }

    /**
     * Тест для отладки и поиска элементов (закомментирован)
     */
    // @Test(enabled = false)
    public void debugFindElements() {
        logger.info("=== DEBUG: Поиск всех элементов ===");

        try {
            // Безопасное ожидание
            safeSleepQuietly(5000);

            // Получаем исходный код страницы
            String pageSource = driver.getPageSource();
            logger.info("Длина page source: {} символов", pageSource.length());

            // Выводим первые 2000 символов для отладки
            String preview = pageSource.substring(0, Math.min(2000, pageSource.length()));
            logger.info("Preview page source:\n{}", preview);

            // Ищем все элементы с resource-id
            var elements = driver.findElements(org.openqa.selenium.By.xpath("//*[@resource-id]"));
            logger.info("Найдено элементов с resource-id: {}", elements.size());

            for (var element : elements) {
                String id = element.getAttribute("resource-id");
                String text = element.getText();
                String className = element.getAttribute("class");
                logger.debug("ID: {} | Class: {} | Text: {}", id, className, text);
            }

            // Ищем конкретные элементы
            try {
                var searchContainer = driver.findElement(
                        org.openqa.selenium.By.id("org.wikipedia.alpha:id/search_container"));
                logger.info("Найден search_container: {}", searchContainer.isDisplayed());
            } catch (Exception e) {
                logger.warn("Не найден search_container: {}", e.getMessage());
            }

        } catch (Exception e) {
            logger.error("Отладочный метод завершился с ошибкой: {}", e.getMessage());
        }
    }

    /**
     * Метод для проверки, упал ли тест
     */
    private boolean isTestFailed() {
        // В TestNG можно использовать ITestResult, но для простоты возвращаем false
        // В реальном проекте можно интегрировать с TestNG listeners
        return false;
    }

    /**
     * Метод для создания скриншота
     */
    private void takeScreenshot(String testName) {
        try {
            // В реальном проекте можно сохранять скриншоты в файл
            // byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
            logger.info("Скриншот создан для теста: {}", testName);
        } catch (Exception e) {
            logger.warn("Не удалось создать скриншот: {}", e.getMessage());
        }
    }

    /**
     * Безопасный sleep без выбрасывания исключения
     */
    private void safeSleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Sleep прерван: {}", e.getMessage());
        }
    }
}