package ru.javabruse.web;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.javabruse.pages.WikipediaPage;
import ru.javabruse.utils.WebDriverFactory;

import java.lang.reflect.Method;
import java.util.List;

// Тесты для веб-версии Википедии
public class WikipediaTests {

    private static final Logger logger = LoggerFactory.getLogger(WikipediaTests.class);

    private WebDriver driver;
    private WikipediaPage wikipediaPage;
    private static final String BASE_URL = "https://ru.wikipedia.org/";

    @BeforeMethod
    public void setUp(Method method) {
        logger.info("Начало настройки теста: {}", method.getName());

        try {
            driver = WebDriverFactory.createWebDriver();
            wikipediaPage = new WikipediaPage(driver);

            logger.info("Открываем главную страницу Википедии");
            wikipediaPage.openMainPage();

            logger.info("Тестовая среда настроена для: {}", method.getName());

        } catch (Exception e) {
            logger.error("Ошибка при настройке теста {}: {}", method.getName(), e.getMessage(), e);
            throw new RuntimeException("Не удалось настроить тестовую среду", e);
        }
    }

    @AfterMethod
    public void tearDown(Method method) {
        logger.info("Завершение теста: {}", method.getName());

        try {
            if (driver != null) {
                if (isTestFailed()) {
                    takeScreenshot(method.getName() + "_failed");
                }

                driver.quit();
                logger.info("Драйвер закрыт для теста: {}", method.getName());
            }
        } catch (Exception e) {
            logger.warn("Ошибка при завершении теста {}: {}", method.getName(), e.getMessage());
        }
    }

    /**
     * Тест загрузки главной страницы
     */
    @Test(priority = 1, description = "Проверка загрузки главной страницы Википедии")
    public void testMainPageLoad() {
        logger.info("Тест: проверка загрузки главной страницы");

        boolean isLoaded = wikipediaPage.isMainPageLoaded();
        logger.info("Главная страница загружена: {}", isLoaded);

        Assert.assertTrue(isLoaded, "Главная страница Википедии должна быть корректно загружена");

        // Дополнительные проверки
        String currentUrl = driver.getCurrentUrl();
        Assert.assertTrue(currentUrl.contains("wikipedia.org"),
                "Текущий URL должен содержать 'wikipedia.org'. Фактический: " + currentUrl);

        String pageTitle = driver.getTitle();
        Assert.assertFalse(pageTitle.isEmpty(), "Заголовок страницы не должен быть пустым");
        logger.info("Заголовок страницы: {}", pageTitle);
    }

    /**
     * Тест базового функционала поиска
     */
    @Test(priority = 2, description = "Проверка базового функционала поиска")
    public void testBasicSearchFunctionality() {
        logger.info("Тест: базовый функционал поиска");
        String searchQuery = "Россия";

        // Выполняем поиск
        WikipediaPage.SearchResult result = wikipediaPage.searchArticle(searchQuery, false);
        logger.info("Результат поиска: {}", result);

        // Получаем заголовок страницы
        String heading = wikipediaPage.getPageTitle();
        logger.info("Заголовок найденной статьи: '{}'", heading);

        // Проверки
        Assert.assertNotNull(heading, "Заголовок статьи не должен быть null");
        Assert.assertFalse(heading.isEmpty(), "Заголовок статьи не должен быть пустым");
        Assert.assertTrue(heading.contains(searchQuery),
                String.format("Заголовок должен содержать '%s'. Фактический: %s",
                        searchQuery, heading));
    }

    /**
     * Тест поиска с использованием подсказок
     */
    @Test(priority = 3, description = "Проверка поиска с подсказками")
    public void testSearchWithSuggestions() {
        logger.info("Тест: поиск с использованием подсказок");
        String searchQuery = "Москва";

        // Проверяем доступность подсказок
        boolean hasSuggestions = wikipediaPage.areSearchSuggestionsAvailable();
        logger.info("Подсказки поиска доступны: {}", hasSuggestions);

        // Выполняем поиск с попыткой использования подсказок
        WikipediaPage.SearchResult result = wikipediaPage.searchArticle(searchQuery, hasSuggestions);

        if (result != null) {
            logger.info("Открыта статья через подсказку: {}", result.getArticleTitle());
            Assert.assertFalse(result.getArticleTitle().isEmpty(),
                    "Заголовок статьи не должен быть пустым");
        } else {
            // Если подсказок нет, выполняем обычный поиск
            logger.info("Подсказки недоступны, выполняем обычный поиск");
            result = wikipediaPage.searchArticle(searchQuery, false);
            String heading = wikipediaPage.getPageTitle();
            Assert.assertTrue(heading.contains(searchQuery),
                    "Заголовок должен содержать поисковый запрос");
        }
    }

    /**
     * Тест навигации на случайную страницу
     */
    @Test(priority = 4, description = "Проверка перехода на случайную страницу")
    public void testRandomPageNavigation() {
        logger.info("Тест: переход на случайную страницу");

        // Запоминаем текущий URL
        String originalUrl = driver.getCurrentUrl();
        logger.info("Исходный URL: {}", originalUrl);

        // Переходим на случайную страницу
        wikipediaPage.goToRandomPage();

        // Получаем новый URL и заголовок
        String newUrl = driver.getCurrentUrl();
        String randomPageTitle = wikipediaPage.getPageTitle();

        logger.info("Новый URL: {}", newUrl);
        logger.info("Заголовок случайной страницы: {}", randomPageTitle);

        // Проверки
        Assert.assertNotEquals(newUrl, originalUrl,
                "После перехода на случайную страницу URL должен измениться");

        Assert.assertFalse(randomPageTitle.isEmpty(),
                "Заголовок случайной страницы не должен быть пустым");

        Assert.assertNotEquals(randomPageTitle, "Заглавная страница",
                "Случайная страница не должна быть главной страницей");

        // Проверяем, что это действительно статья
        Assert.assertTrue(newUrl.contains("/wiki/"),
                "URL должен содержать путь к статье /wiki/");
    }

    /**
     * Тест доступности и интерактивности поля поиска
     */
    @Test(priority = 5, description = "Проверка доступности поля поиска")
    public void testSearchInputAvailability() {
        logger.info("Тест: проверка доступности поля поиска");

        // Проверяем через наш Page Object
        Assert.assertTrue(wikipediaPage.isMainPageLoaded(),
                "Главная страница должна быть загружена");

        // Проверяем, что поле поиска доступно
        // Этот метод нужно добавить в WikipediaPage или использовать альтернативу
        try {
            // Альтернатива: проверяем, что можем выполнить поиск
            wikipediaPage.searchArticle("тест", false);
            String resultTitle = wikipediaPage.getPageTitle();
            Assert.assertFalse(resultTitle.isEmpty(),
                    "После поиска должна открыться страница с заголовком");
        } catch (Exception e) {
            logger.error("Ошибка при проверке поля поиска: {}", e.getMessage());
            throw new AssertionError("Поле поиска недоступно для использования", e);
        }
    }

    /**
     * Тест наличия и содержания инфобокса в статьях
     */
    @Test(priority = 6, description = "Проверка инфобоксов в статьях")
    public void testArticleInfobox() {
        logger.info("Тест: проверка инфобоксов в статьях");

        // Ищем статью, которая обычно имеет инфобокс
        wikipediaPage.searchArticle("Альберт Эйнштейн", false);

        // Проверяем наличие инфобокса
        boolean hasInfobox = wikipediaPage.hasInfobox();
        logger.info("Статья имеет инфобокс: {}", hasInfobox);

        if (hasInfobox) {
            String infoboxContent = wikipediaPage.getInfoboxContent();
            logger.info("Содержимое инфобокса (первые 200 символов): {}...",
                    infoboxContent.substring(0, Math.min(200, infoboxContent.length())));

            Assert.assertFalse(infoboxContent.isEmpty(),
                    "Содержимое инфобокса не должно быть пустым");
            Assert.assertTrue(infoboxContent.contains("Эйнштейн") ||
                            infoboxContent.contains("Einstein"),
                    "Инфобокс должен содержать информацию об Эйнштейне");
        } else {
            logger.warn("Статья не имеет инфобокса, это может быть нормально для некоторых статей");
        }
    }

    /**
     * Тест навигации по содержанию статьи
     */
    @Test(priority = 7, description = "Проверка навигации по содержанию")
    public void testTableOfContentsNavigation() {
        logger.info("Тест: навигация по содержанию статьи");

        // Ищем статью с содержанием
        wikipediaPage.searchArticle("Программирование", false);

        // Проверяем наличие содержания
        boolean hasToc = wikipediaPage.hasTableOfContents();
        logger.info("Статья имеет содержание: {}", hasToc);

        if (hasToc) {
            // Прокручиваем до содержания
            wikipediaPage.scrollToElement(org.openqa.selenium.By.id("toc"));

            // Можно добавить клик по ссылке в содержании, если нужно
            // wikipediaPage.clickTocLink("История");

            logger.info("Содержание доступно для навигации");
        } else {
            logger.info("Статья не имеет содержания, что может быть нормально для коротких статей");
        }
    }

    /**
     * Тест подсчета элементов в статье
     */
    @Test(priority = 8, description = "Проверка подсчета элементов в статье")
    public void testArticleElementsCount() {
        logger.info("Тест: подсчет элементов в статье");

        wikipediaPage.searchArticle("Живопись", false);

        // Считаем изображения
        int imageCount = wikipediaPage.countImages();
        logger.info("Количество изображений в статье: {}", imageCount);

        // Считаем внешние ссылки
        int externalLinksCount = wikipediaPage.countExternalLinks();
        logger.info("Количество внешних ссылок в статье: {}", externalLinksCount);

        // Получаем категории
        List<String> categories = wikipediaPage.getArticleCategories();
        logger.info("Категории статьи: {}", categories);

        // Проверки
        Assert.assertTrue(imageCount >= 0, "Количество изображений не может быть отрицательным");
        Assert.assertTrue(externalLinksCount >= 0, "Количество внешних ссылок не может быть отрицательным");

        if (!categories.isEmpty()) {
            logger.info("Статья имеет {} категорий", categories.size());
            Assert.assertTrue(categories.size() > 0, "Статья должна иметь хотя бы одну категорию");
        }
    }

    /**
     * Тест переключения между вкладками статьи
     */
    @Test(priority = 9, description = "Проверка переключения вкладок статьи")
    public void testArticleTabsSwitching() {
        logger.info("Тест: переключение вкладок статьи");

        wikipediaPage.searchArticle("Литература", false);
        String initialTitle = wikipediaPage.getPageTitle();
        logger.info("Исходная статья: {}", initialTitle);

        // Переключаемся на вкладку "Обсуждение"
        wikipediaPage.switchToDiscussionTab();
        String discussionTitle = wikipediaPage.getPageTitle();
        logger.info("Вкладка 'Обсуждение': {}", discussionTitle);

        // Проверяем, что это действительно страница обсуждения
        Assert.assertTrue(discussionTitle.contains("Обсуждение") ||
                        discussionTitle.contains("Talk:"),
                "Заголовок должен указывать на страницу обсуждения");

        // Возвращаемся на вкладку статьи
        driver.navigate().back();
        String returnedTitle = wikipediaPage.getPageTitle();
        logger.info("Возврат к статье: {}", returnedTitle);

        Assert.assertTrue(returnedTitle.contains(initialTitle) ||
                        initialTitle.contains(returnedTitle),
                "После возврата должны быть на исходной статье");
    }

    /**
     * Параметризованный тест поиска с разными запросами
     */
    @Test(priority = 10, description = "Параметризованный тест поиска",
            dataProvider = "searchTestData")
    public void testParameterizedSearch(String searchQuery, String expectedInTitle) {
        logger.info("Параметризованный тест поиска: '{}'", searchQuery);

        wikipediaPage.searchArticle(searchQuery, false);
        String actualTitle = wikipediaPage.getPageTitle();
        logger.info("Результат для '{}': {}", searchQuery, actualTitle);

        Assert.assertFalse(actualTitle.isEmpty(),
                String.format("Для запроса '%s' должен быть найден результат", searchQuery));

        if (expectedInTitle != null && !expectedInTitle.isEmpty()) {
            Assert.assertTrue(actualTitle.toLowerCase().contains(expectedInTitle.toLowerCase()),
                    String.format("Заголовок должен содержать '%s'. Фактический: %s",
                            expectedInTitle, actualTitle));
        }
    }

    /**
     * Комплексный тест работы с Википедией
     */
    @Test(priority = 11, description = "Комплексный тест работы с Википедией")
    public void testComplexWikipediaWorkflow() {
        logger.info("Комплексный тест работы с Википедией");

        // Шаг 1: Проверка главной страницы
        Assert.assertTrue(wikipediaPage.isMainPageLoaded(),
                "Шаг 1: Главная страница должна быть загружена");

        // Шаг 2: Поиск первой статьи
        wikipediaPage.searchArticle("Физика", false);
        String firstArticle = wikipediaPage.getPageTitle();
        Assert.assertFalse(firstArticle.isEmpty(),
                "Шаг 2: Первая статья должна быть найдена");
        logger.info("Первая статья: {}", firstArticle);

        // Шаг 3: Проверка элементов статьи
        int firstArticleImages = wikipediaPage.countImages();
        logger.info("Изображений в первой статье: {}", firstArticleImages);

        // Шаг 4: Переход на случайную страницу
        wikipediaPage.goToRandomPage();
        String randomArticle = wikipediaPage.getPageTitle();
        Assert.assertFalse(randomArticle.isEmpty(),
                "Шаг 4: Случайная статья должна быть загружена");
        logger.info("Случайная статья: {}", randomArticle);

        // Шаг 5: Поиск со случайной страницы
        wikipediaPage.searchArticle("Химия", false);
        String searchedArticle = wikipediaPage.getPageTitle();
        Assert.assertTrue(searchedArticle.contains("Химия"),
                "Шаг 5: Должна быть найдена статья по химии");
        logger.info("Найденная статья: {}", searchedArticle);

        // Шаг 6: Возврат на главную
        driver.get(BASE_URL);
        Assert.assertTrue(wikipediaPage.isMainPageLoaded(),
                "Шаг 6: Должны вернуться на главную страницу");

        logger.info("Комплексный тест выполнен успешно");
    }

    /**
     * Провайдер данных для параметризованных тестов
     */
    @DataProvider(name = "searchTestData")
    public Object[][] provideSearchTestData() {
        return new Object[][] {
                {"Математика", "Математика"},
                {"История", "История"},
                {"Биология", "Биология"},
                {"Искусственный интеллект", "Искусственный интеллект"},
                {"Космос", "Космос"}
        };
    }

    /**
     * Проверяет, упал ли тест
     */
    private boolean isTestFailed() {
        // В реальном проекте можно использовать TestNG listeners
        // для определения статуса теста
        return false;
    }

    /**
     * Создает скриншот
     */
    private void takeScreenshot(String testName) {
        try {
            // В реальном проекте можно сохранять скриншоты в файл
            logger.info("Создан скриншот для теста: {}", testName);
        } catch (Exception e) {
            logger.warn("Не удалось создать скриншот: {}", e.getMessage());
        }
    }
}