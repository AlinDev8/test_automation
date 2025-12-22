package ru.javabruse.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WikipediaPage {

    private static final Logger logger = LoggerFactory.getLogger(WikipediaPage.class);
    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final Actions actions;

    // Базовые URL
    private static final String BASE_URL = "https://ru.wikipedia.org";
    private static final String MAIN_PAGE_URL = BASE_URL + "/wiki/Заглавная_страница";
    private static final String RANDOM_PAGE_URL = BASE_URL + "/wiki/Special:Random";

    private final By WIKI_LOGO = By.cssSelector("div#p-logo a");
    private final By PAGE_HEADING = By.id("firstHeading");
    private final By PAGE_SUBHEADING = By.id("siteSub");
    private final By BODY_CONTENT = By.id("bodyContent");

    // Серч
    private final By SEARCH_INPUT = By.id("searchInput");
    private final By SEARCH_BUTTON = By.cssSelector("#searchform button");
    private final By SEARCH_SUGGESTIONS = By.cssSelector(".suggestions-results a");
    private final By SEARCH_RESULTS = By.cssSelector(".mw-search-results li");

    private final By MAIN_MENU = By.id("p-navigation");
    private final By SIDE_MENU = By.id("p-views");
    private final By RANDOM_PAGE_LINK = By.id("n-randompage");
    private final By RECENT_CHANGES_LINK = By.id("n-recentchanges");

    // Содержимое статьи
    private final By ARTICLE_CONTENT = By.id("mw-content-text");
    private final By INFOBOX = By.cssSelector(".infobox");
    private final By TABLE_OF_CONTENTS = By.id("toc");
    private final By CATEGORIES = By.id("catlinks");

    private final By LANGUAGE_SWITCHER = By.id("p-lang");
    private final By LANGUAGE_OPTIONS = By.cssSelector("#p-lang .uls-settings-trigger");

    // Вкладки
    private final By ARTICLE_TAB = By.id("ca-nstab-main");
    private final By DISCUSSION_TAB = By.id("ca-talk");
    private final By EDIT_TAB = By.id("ca-edit");
    private final By HISTORY_TAB = By.id("ca-history");

    // Специальные элементы
    private final By COORDINATES = By.cssSelector(".geo-dms, .geo-dec");
    private final By EXTERNAL_LINKS = By.cssSelector("a.external");
    private final By REFERENCES = By.cssSelector(".references");
    private final By IMAGES = By.cssSelector(".image img, .thumb img");

    public WikipediaPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        this.actions = new Actions(driver);
    }

    public void openMainPage() {
        logger.info("Открываем главную страницу Википедии");
        driver.get(MAIN_PAGE_URL);
        waitForPageLoad();
        acceptCookiesIfPresent();
    }

    public boolean isMainPageLoaded() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(WIKI_LOGO));
            wait.until(ExpectedConditions.visibilityOfElementLocated(PAGE_HEADING));
            return driver.getCurrentUrl().contains("Заглавная_страница");
        } catch (TimeoutException e) {
            logger.error("Главная страница не загрузилась: {}", e.getMessage());
            return false;
        }
    }

    // Поиск статьи с расширенными опциями
    public SearchResult searchArticle(String query, boolean useSuggestions) {
        logger.info("Выполняем поиск статьи: '{}'", query);

        try {
            WebElement searchInput = wait.until(ExpectedConditions.elementToBeClickable(SEARCH_INPUT));
            searchInput.clear();
            searchInput.sendKeys(query);

            if (useSuggestions && areSearchSuggestionsAvailable()) {
                logger.info("Используем поисковые подсказки");
                return selectFirstSearchSuggestion();
            } else {
                logger.info("Выполняем стандартный поиск");
                searchInput.sendKeys(Keys.RETURN);
                waitForPageLoad();
                return new SearchResult(getSearchResultsCount(), getPageTitle());
            }
        } catch (Exception e) {
            logger.error("Ошибка при поиске статьи: {}", e.getMessage());
            throw e;
        }
    }

    public SearchResult selectFirstSearchSuggestion() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH_SUGGESTIONS));
            List<WebElement> suggestions = driver.findElements(SEARCH_SUGGESTIONS);

            if (!suggestions.isEmpty()) {
                String suggestionText = suggestions.get(0).getText();
                logger.info("Выбираем подсказку: {}", suggestionText);
                suggestions.get(0).click();
                waitForPageLoad();
                return new SearchResult(1, getPageTitle());
            }
        } catch (TimeoutException e) {
            logger.warn("Подсказки поиска не найдены");
        }
        return null;
    }

    public boolean areSearchSuggestionsAvailable() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            return !shortWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(SEARCH_SUGGESTIONS)).isEmpty();
        } catch (TimeoutException e) {
            return false;
        }
    }

    // Рандом страница
    public void goToRandomPage() {
        logger.info("Переходим на случайную страницу");
        try {
            driver.get(RANDOM_PAGE_URL);
            waitForPageLoad();
            logger.info("Открыта страница: {}", getPageTitle());
        } catch (Exception e) {
            logger.error("Ошибка при переходе на случайную страницу: {}", e.getMessage());
            // Альтернативный способ через клик по ссылке
            driver.findElement(RANDOM_PAGE_LINK).click();
            waitForPageLoad();
        }
    }

    // заголовок текущей страницы
    public String getPageTitle() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(PAGE_HEADING)).getText().trim();
        } catch (Exception e) {
            logger.error("Не удалось получить заголовок страницы: {}", e.getMessage());
            return "";
        }
    }

    public boolean hasInfobox() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            return !shortWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(INFOBOX)).isEmpty();
        } catch (TimeoutException e) {
            return false;
        }
    }

    public String getInfoboxContent() {
        if (hasInfobox()) {
            return driver.findElement(INFOBOX).getText();
        }
        return "Инфобокс не найден";
    }

    public boolean hasTableOfContents() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(TABLE_OF_CONTENTS)).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    public void clickTocLink(String linkText) {
        try {
            By tocLink = By.xpath("//div[@id='toc']//a[contains(text(), '" + linkText + "')]");
            WebElement link = wait.until(ExpectedConditions.elementToBeClickable(tocLink));
            link.click();
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("top")));
        } catch (Exception e) {
            logger.error("Не удалось найти ссылку в содержании: {}", linkText);
        }
    }

    // Переключения между вкладками:
    public void switchToDiscussionTab() {
        switchTab(DISCUSSION_TAB, "Обсуждение");
    }

    public void switchToEditTab() {
        switchTab(EDIT_TAB, "Правка");
    }

    public void switchToHistoryTab() {
        switchTab(HISTORY_TAB, "История");
    }

    private void switchTab(By tabLocator, String tabName) {
        try {
            logger.info("Переключаемся на вкладку: {}", tabName);
            WebElement tab = wait.until(ExpectedConditions.elementToBeClickable(tabLocator));
            tab.click();
            waitForPageLoad();
        } catch (Exception e) {
            logger.error("Не удалось переключиться на вкладку {}: {}", tabName, e.getMessage());
        }
    }
    // конец переключения между вкладками

    // количество изображений в статье
    public int countImages() {
        try {
            List<WebElement> images = wait.until(
                    ExpectedConditions.presenceOfAllElementsLocatedBy(IMAGES)
            );
            return images.size();
        } catch (TimeoutException e) {
            return 0;
        }
    }

    // количество внешних ссылок
    public int countExternalLinks() {
        try {
            List<WebElement> links = driver.findElements(EXTERNAL_LINKS);
            return links.size();
        } catch (Exception e) {
            return 0;
        }
    }

    // координаты
    public String getCoordinates() {
        try {
            return driver.findElement(COORDINATES).getText();
        } catch (NoSuchElementException e) {
            return "Координаты не найдены";
        }
    }

    // категории статьи
    public List<String> getArticleCategories() {
        List<String> categories = new ArrayList<>();
        try {
            List<WebElement> categoryElements = driver.findElements(By.cssSelector("#catlinks ul li a"));

            categories = categoryElements.stream()
                    .map(WebElement::getText)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.warn("Не удалось получить категории статьи: {}", e.getMessage());
        }
        return categories;
    }

    // категории статьи (alt)
    public List<String> getArticleCategoriesAlternative() {
        List<String> categories = new ArrayList<>();
        try {
            List<WebElement> categoryElements = driver.findElements(By.cssSelector("#catlinks ul li a"));

            // Способ 2: Классический цикл
            for (WebElement category : categoryElements) {
                categories.add(category.getText());
            }

        } catch (Exception e) {
            logger.warn("Не удалось получить категории статьи: {}", e.getMessage());
        }
        return categories;
    }

    // количество результатов поиска
    public int getSearchResultsCount() {
        try {
            WebElement resultsInfo = driver.findElement(By.cssSelector(".results-info"));
            String text = resultsInfo.getText();
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            try {
                List<WebElement> results = driver.findElements(SEARCH_RESULTS);
                return results.size();
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    public void scrollToElement(By locator) {
        try {
            WebElement element = driver.findElement(locator);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            waitForPageSettle(500);
        } catch (Exception e) {
            logger.warn("Не удалось прокрутить до элемента: {}", e.getMessage());
        }
    }

    public void scrollToBottom() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "window.scrollTo(0, document.body.scrollHeight);"
            );
            waitForPageSettle(1000);
        } catch (Exception e) {
            logger.warn("Не удалось прокрутить страницу: {}", e.getMessage());
        }
    }

    // скриншот страницы
    public byte[] takeScreenshot() {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    public boolean isSearchInputAvailable() {
        try {
            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH_INPUT));
            return searchInput.isDisplayed() && searchInput.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    private void waitForPageLoad() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(BODY_CONTENT));
            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete")
            );
            waitForPageSettle(1000);
        } catch (TimeoutException e) {
            logger.warn("Страница загрузилась не полностью: {}", e.getMessage());
        }
    }

    private void waitForPageSettle(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // куки
    private void acceptCookiesIfPresent() {
        try {
            By cookieBanner = By.cssSelector(".mw-cookiewarning-container, .cookie-banner");
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            WebElement banner = shortWait.until(ExpectedConditions.visibilityOfElementLocated(cookieBanner));

            if (banner.isDisplayed()) {
                WebElement acceptButton = banner.findElement(By.tagName("button"));
                acceptButton.click();
                logger.info("Куки приняты");
                waitForPageSettle(500);
            }
        } catch (TimeoutException e) {
        }
    }

    // результаты поиска
    public static class SearchResult {
        private final int resultsCount;
        private final String articleTitle;

        public SearchResult(int resultsCount, String articleTitle) {
            this.resultsCount = resultsCount;
            this.articleTitle = articleTitle;
        }

        public int getResultsCount() {
            return resultsCount;
        }

        public String getArticleTitle() {
            return articleTitle;
        }

        @Override
        public String toString() {
            return "SearchResult{" +
                    "resultsCount=" + resultsCount +
                    ", articleTitle='" + articleTitle + '\'' +
                    '}';
        }
    }
}