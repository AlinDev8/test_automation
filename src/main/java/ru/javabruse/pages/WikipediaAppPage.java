package ru.javabruse.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public class WikipediaAppPage {

    private static final Logger logger = LoggerFactory.getLogger(WikipediaAppPage.class);
    private final AndroidDriver driver;
    private final WebDriverWait wait;

    @AndroidFindBy(id = "org.wikipedia.alpha:id/search_container")
    private WebElement searchContainer;

    @AndroidFindBy(id = "org.wikipedia.alpha:id/search_src_text")
    private WebElement searchInputField;

    @AndroidFindBy(id = "org.wikipedia.alpha:id/page_list_item_title")
    private List<WebElement> resultTitles;

    @AndroidFindBy(id = "org.wikipedia.alpha:id/fragment_onboarding_skip_button")
    private WebElement skipButton;

    @AndroidFindBy(id = "org.wikipedia.alpha:id/view_announcement_action_negative")
    private WebElement closePopupButton;

    @AndroidFindBy(uiAutomator = "new UiSelector().description(\"Navigate up\")")
    private WebElement navigateUpButton;

    @AndroidFindBy(className = "android.widget.TextView")
    private List<WebElement> textViews;

    @AndroidFindBy(id = "org.wikipedia.alpha:id/view_article_header_title")
    private WebElement articleTitleHeader;

    private final By NAVIGATE_UP_BY_ACCESSIBILITY = AppiumBy.accessibilityId("Navigate up");
    private final By NAVIGATE_UP_BY_XPATH = By.xpath("//android.widget.ImageButton[@content-desc='Navigate up']");

    public WikipediaAppPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        PageFactory.initElements(new AppiumFieldDecorator(driver), this);
    }

    /**
     * Получаем кнопку навигации с использованием accessibilityId
     */
    public WebElement getNavigateUpButton() {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(navigateUpButton));
        } catch (TimeoutException e) {
            logger.warn("Не удалось найти кнопку через UiAutomator, пробуем другие методы...");

            // Альтернативная стратегия - поиск через стандартный локатор
            try {
                return driver.findElement(NAVIGATE_UP_BY_ACCESSIBILITY);
            } catch (Exception ex) {
                // Альтернативная стратегия 2 - поиск через xpath
                return driver.findElement(NAVIGATE_UP_BY_XPATH);
            }
        }
    }

    /**
     * Проверка доступа кнопки навигации
     */
    public boolean isNavigateUpButtonDisplayed() {
        try {
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            return quickWait.until(ExpectedConditions.presenceOfElementLocated(
                    NAVIGATE_UP_BY_ACCESSIBILITY
            )).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void skipOnboardingIfPresent() {
        try {
            WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement skipBtn = quickWait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            AppiumBy.id("org.wikipedia.alpha:id/fragment_onboarding_skip_button")
                    )
            );

            if (skipBtn.isDisplayed()) {
                logger.info("Найден онбординг, пропускаем...");
                skipBtn.click();
                waitForAppToSettle(1000);
            }
        } catch (TimeoutException e) {
            logger.info("Онбординг не найден, продолжаем...");
        }
    }

    //Универсальный метод для закрытия всплывающих окон
    public void dismissAllPopups() {
        try {
            String[] popupSelectors = {
                    "org.wikipedia.alpha:id/view_announcement_action_negative",
                    "org.wikipedia.alpha:id/closeButton",
                    "org.wikipedia.alpha:id/dialogContainer"
            };

            for (String selector : popupSelectors) {
                try {
                    WebDriverWait quickWait = new WebDriverWait(driver, Duration.ofSeconds(2));
                    List<WebElement> popups = quickWait.until(
                            ExpectedConditions.presenceOfAllElementsLocatedBy(AppiumBy.id(selector))
                    );

                    if (!popups.isEmpty() && popups.get(0).isDisplayed()) {
                        logger.info("Закрываем всплывающее окно с селектором: {}", selector);
                        popups.get(0).click();
                        waitForAppToSettle(1000);
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {
            logger.warn("Ошибка при закрытии всплывающих окон: {}", e.getMessage());
        }
    }

    public boolean isMainScreenLoaded() {
        try {
            skipOnboardingIfPresent();
            dismissAllPopups();

            return wait.until(ExpectedConditions.elementToBeClickable(searchContainer)).isDisplayed();
        } catch (Exception e) {
            logger.error("Главный экран не загрузился: {}", e.getMessage());
            return false;
        }
    }

    public void searchArticle(String query) {
        logger.info("Выполняем поиск статьи: {}", query);

        try {
            skipOnboardingIfPresent();
            dismissAllPopups();

            wait.until(ExpectedConditions.elementToBeClickable(searchContainer)).click();
            waitForAppToSettle(500);

            wait.until(ExpectedConditions.visibilityOf(searchInputField)).sendKeys(query);
            waitForAppToSettle(1500);

            wait.until(driver -> !resultTitles.isEmpty());

            if (!resultTitles.isEmpty()) {
                logger.info("Найдено результатов: {}", resultTitles.size());
                resultTitles.get(0).click();
            } else {
                logger.warn("Результаты поиска не найдены");
                throw new RuntimeException("Результаты поиска не найдены для запроса: " + query);
            }

            waitForArticleToLoad();

        } catch (Exception e) {
            logger.error("Ошибка при поиске статьи: {}", e.getMessage());
            throw e;
        }
    }

    // заголовок статьи
    public String getArticleTitle() {
        try {
            dismissAllPopups();

            try {
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
                WebElement title = shortWait.until(ExpectedConditions.visibilityOf(articleTitleHeader));
                return title.getText().trim();
            } catch (TimeoutException e) {
                logger.info("Используем альтернативную стратегию поиска заголовка");
            }

            if (!textViews.isEmpty()) {
                for (WebElement textView : textViews) {
                    if (textView.isDisplayed()) {
                        String text = textView.getText().trim();
                        if (!text.isEmpty() && text.length() > 3 && text.length() < 100) {
                            return text;
                        }
                    }
                }
            }

            String xpathTitle = driver.findElement(AppiumBy.xpath(
                    "//android.widget.TextView[contains(@text, '')][1]"
            )).getText();

            if (!xpathTitle.trim().isEmpty()) {
                return xpathTitle.trim();
            }

            throw new RuntimeException("Заголовок статьи не найден");

        } catch (Exception e) {
            logger.error("Не удалось получить заголовок статьи: {}", e.getMessage());
            return "";
        }
    }

    // бэк на предыдущий экран
    public void goBack() {
        try {
            logger.info("Возвращаемся назад");
            dismissAllPopups();

            if (isNavigateUpButtonDisplayed()) {
                WebElement navButton = getNavigateUpButton();
                navButton.click();
                logger.info("Нажата кнопка навигации");
            } else {
                driver.navigate().back();
                logger.info("Использована системная кнопка 'назад'");
            }

            waitForAppToSettle(1500);
            dismissAllPopups();

        } catch (Exception e) {
            logger.warn("Ошибка при возврате назад: {}", e.getMessage());
            driver.navigate().back();
        }
    }

    // загрузка статьи
    private void waitForArticleToLoad() {
        try {
            wait.until(driver -> {
                try {
                    dismissAllPopups();
                    return !textViews.isEmpty() &&
                            textViews.stream().anyMatch(WebElement::isDisplayed);
                } catch (Exception e) {
                    return false;
                }
            });
            waitForAppToSettle(1000);
        } catch (Exception e) {
            logger.warn("Ожидание загрузки статьи завершилось с ошибкой: {}", e.getMessage());
        }
    }

    private void waitForAppToSettle(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getSearchResultsCount() {
        try {
            wait.until(driver -> !resultTitles.isEmpty());
            return resultTitles.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public void selectSearchResult(int index) {
        try {
            wait.until(driver -> resultTitles.size() > index);
            if (index < resultTitles.size()) {
                resultTitles.get(index).click();
                waitForArticleToLoad();
            }
        } catch (Exception e) {
            logger.error("Не удалось выбрать результат с индексом {}: {}", index, e.getMessage());
            throw e;
        }
    }

    // очистка поля поиска
    public void clearSearchField() {
        try {
            if (searchInputField.isDisplayed()) {
                searchInputField.clear();
                waitForAppToSettle(500);
            }
        } catch (Exception e) {
            logger.warn("Не удалось очистить поле поиска: {}", e.getMessage());
        }
    }
}