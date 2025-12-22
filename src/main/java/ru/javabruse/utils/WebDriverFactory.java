package ru.javabruse.utils;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.remote.AutomationName;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class WebDriverFactory {

    private static final Logger logger = LoggerFactory.getLogger(WebDriverFactory.class);

    // Константы для Appium
    private static final String APPIUM_SERVER_URL = "http://127.0.0.1:4723";
    private static final String WIKIPEDIA_APP_PACKAGE = "org.wikipedia.alpha";
    private static final String WIKIPEDIA_MAIN_ACTIVITY = "org.wikipedia.main.MainActivity";

    // Константы для конфы
    private static final long IMPLICIT_WAIT_SECONDS = 10;
    private static final long PAGE_LOAD_TIMEOUT_SECONDS = 30;
    private static final long SCRIPT_TIMEOUT_SECONDS = 15;

    public static WebDriver createWebDriver() {
        String browser = System.getProperty("browser", "chrome").toLowerCase();
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "false"));

        logger.info("Драйвер для браузера: {} (headless: {})", browser, headless);

        WebDriver driver;

        switch (browser) {
            case "firefox":
                driver = createFirefoxDriver(headless);
                break;
            case "edge":
                driver = createEdgeDriver(headless);
                break;
            case "safari":
                driver = createSafariDriver();
                break;
            case "chrome":
            default:
                driver = createChromeDriver(headless);
                break;
        }

        configureDriver(driver);
        return driver;
    }

    // ChromeDriver с расширенными опциями
    private static ChromeDriver createChromeDriver(boolean headless) {
        ChromeOptions options = new ChromeOptions();

        // Базовые опции
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");

        // Экспериментальные опции
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        // Headless
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
            logger.info("Запуск Chrome в headless режиме");
        }

        // Мобильная эмуляция
        String mobileEmulation = System.getProperty("mobile.emulation");
        if (mobileEmulation != null) {
            Map<String, Object> mobileEmulationMap = new HashMap<>();
            mobileEmulationMap.put("deviceName", mobileEmulation);
            options.setExperimentalOption("mobileEmulation", mobileEmulationMap);
            logger.info("Включена мобильная эмуляция: {}", mobileEmulation);
        }

        // Расширения
        if (Boolean.parseBoolean(System.getProperty("chrome.extensions", "false"))) {
            // сейв образца, если понадобится
            // options.addExtensions(new File("path/to/extension.crx"));
        }

        return new ChromeDriver(options);
    }

    // FirefoxDriver с расширенными опциями
    private static FirefoxDriver createFirefoxDriver(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();

        if (headless) {
            options.addArguments("--headless");
            logger.info("Запуск Firefox в headless режиме");
        }

        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("browser.cache.disk.enable", false);
        options.addPreference("browser.cache.memory.enable", false);

        return new FirefoxDriver(options);
    }

    // EdgeDriver с расширенными опциями
    private static EdgeDriver createEdgeDriver(boolean headless) {
        EdgeOptions options = new EdgeOptions();

        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--remote-allow-origins=*");

        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
            logger.info("Запуск Edge в headless режиме");
        }

        return new EdgeDriver(options);
    }

    // SafariDriver
    private static SafariDriver createSafariDriver() {
        SafariOptions options = new SafariOptions();
        // Safari имеет ограниченные опции по сравнению с другими браузерами
        return new SafariDriver(options);
    }

    // общие параметры драйвера
    private static void configureDriver(WebDriver driver) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(IMPLICIT_WAIT_SECONDS));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(PAGE_LOAD_TIMEOUT_SECONDS));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(SCRIPT_TIMEOUT_SECONDS));

        // Управление окном
        try {
            driver.manage().window().maximize();
        } catch (Exception e) {
            logger.warn("Не удалось развернуть окно на весь экран: {}", e.getMessage());
        }

        logger.info("Драйвер успешно создан и сконфигурирован");
    }

    // AndroidDriver для мобильного тестирования
    public static AndroidDriver createAndroidDriver() {
        try {
            logger.info("Создаем AndroidDriver для приложения Wikipedia");

            UiAutomator2Options options = new UiAutomator2Options()
                    .setPlatformName("Android")
                    .setAutomationName(AutomationName.ANDROID_UIAUTOMATOR2)
                    .setAppPackage(WIKIPEDIA_APP_PACKAGE)
                    .setAppActivity(WIKIPEDIA_MAIN_ACTIVITY)
                    .setNoReset(false)
                    .setFullReset(false)
                    .setAutoGrantPermissions(true)
                    .setUdid(getDeviceUdid())
                    .setDeviceName(getDeviceName())
                    .setPlatformVersion(getPlatformVersion())
                    .setAvd(getAvdName())
                    .setAvdLaunchTimeout(Duration.ofSeconds(120))
                    .setAvdReadyTimeout(Duration.ofSeconds(120))
                    .setNewCommandTimeout(Duration.ofSeconds(60))
                    .setIsHeadless(isHeadlessEmulator());

            Map<String, Object> additionalCaps = new HashMap<>();
            additionalCaps.put("unicodeKeyboard", true);
            additionalCaps.put("resetKeyboard", true);
            additionalCaps.put("autoAcceptAlerts", true);
            additionalCaps.put("autoDismissAlerts", true);

            options.setCapability("appium:options", additionalCaps);

            logger.info("Подключаемся к Appium серверу: {}", APPIUM_SERVER_URL);
            logger.info("Параметры подключения: {}", options.asMap());

            AndroidDriver driver = new AndroidDriver(new URL(APPIUM_SERVER_URL), options);

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(IMPLICIT_WAIT_SECONDS));

            logger.info("AndroidDriver успешно создан");
            return driver;

        } catch (Exception e) {
            logger.error("Ошибка при создании AndroidDriver: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось создать AndroidDriver", e);
        }
    }

    // AndroidDriver с использованием APK файла
    public static AndroidDriver createAndroidDriverWithApk(String apkPath) {
        try {
            logger.info("Создаем AndroidDriver с установкой APK: {}", apkPath);

            UiAutomator2Options options = new UiAutomator2Options()
                    .setPlatformName("Android")
                    .setAutomationName(AutomationName.ANDROID_UIAUTOMATOR2)
                    .setApp(apkPath)
                    .setNoReset(false)
                    .setFullReset(true)
                    .setAutoGrantPermissions(true)
                    .setUdid(getDeviceUdid())
                    .setDeviceName(getDeviceName());

            return new AndroidDriver(new URL(APPIUM_SERVER_URL), options);

        } catch (Exception e) {
            logger.error("Ошибка при создании AndroidDriver с APK: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось создать AndroidDriver с APK", e);
        }
    }

    // RemoteWebDriver для запуска в  облачных сервисах
    public static RemoteWebDriver createRemoteDriver(String hubUrl, DesiredCapabilities capabilities) {
        try {
            logger.info("Создаем RemoteWebDriver для hub: {}", hubUrl);
            return new RemoteWebDriver(new URL(hubUrl), capabilities);
        } catch (Exception e) {
            logger.error("Ошибка при создании RemoteWebDriver: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось создать RemoteWebDriver", e);
        }
    }

    // Получает UDID устройства
    private static String getDeviceUdid() {
        return System.getProperty("device.udid",
                System.getenv().getOrDefault("ANDROID_DEVICE_UDID", "emulator-5554"));
    }

    // имя устройства из системных свойств или переменных окружения
    private static String getDeviceName() {
        return System.getProperty("device.name",
                System.getenv().getOrDefault("ANDROID_DEVICE_NAME", "Android Emulator"));
    }

    // версия платформы Android
    private static String getPlatformVersion() {
        return System.getProperty("platform.version",
                System.getenv().getOrDefault("ANDROID_PLATFORM_VERSION", "11.0"));
    }

    // AVD
    private static String getAvdName() {
        return System.getProperty("avd.name",
                System.getenv().getOrDefault("ANDROID_AVD_NAME", ""));
    }

    private static boolean isHeadlessEmulator() {
        return Boolean.parseBoolean(System.getProperty("avd.headless", "false"));
    }

    // закрыть
    public static void quitDriver(WebDriver driver) {
        if (driver != null) {
            try {
                logger.info("Закрываем драйвер");
                driver.quit();
                logger.info("Драйвер успешно закрыт");
            } catch (Exception e) {
                logger.warn("Ошибка при закрытии драйвера: {}", e.getMessage());
            }
        }
    }

    // перезапустить
    public static WebDriver restartDriver(WebDriver currentDriver) {
        quitDriver(currentDriver);
        return createWebDriver();
    }

    public static <T> T executeWithRetry(Supplier<WebDriver> driverSupplier,
                                         ThrowingFunction<WebDriver, T> function,
                                         int maxRetries) {
        WebDriver driver = null;
        int retries = 0;

        while (retries <= maxRetries) {
            try {
                driver = driverSupplier.get();
                return function.apply(driver);
            } catch (Exception e) {
                logger.warn("Ошибка выполнения (попытка {} из {}): {}",
                        retries + 1, maxRetries + 1, e.getMessage());

                if (retries == maxRetries) {
                    throw new RuntimeException("Не удалось выполнить операцию после " +
                            (maxRetries + 1) + " попыток", e);
                }

                quitDriver(driver);
                retries++;

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                quitDriver(driver);
            }
        }

        throw new RuntimeException("Не удалось выполнить операцию");
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }
}