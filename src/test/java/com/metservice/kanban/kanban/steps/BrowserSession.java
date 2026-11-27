package com.metservice.kanban.kanban.steps;

import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * One browser per scenario, shared by every step class through cucumber-picocontainer.
 * Chrome is started lazily, so scenarios that never touch the browser don't launch it.
 */
public class BrowserSession {

    public static final String BASE_URL = "http://localhost:8080/kanban";

    private WebDriver driver;
    private WebDriverWait wait;

    public WebDriver driver() {
        if (driver == null) {
            ChromeOptions options = new ChromeOptions();
            // comment the line below to see the browser
            options.addArguments("--headless=new");
            driver = new ChromeDriver(options);
            wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        }
        return driver;
    }

    public WebDriverWait await() {
        driver();
        return wait;
    }

    void quit() {
        if (driver != null) {
            driver.quit();
            driver = null;
            wait = null;
        }
    }
}
