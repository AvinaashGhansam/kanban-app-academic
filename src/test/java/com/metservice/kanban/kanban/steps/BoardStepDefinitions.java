package com.metservice.kanban.kanban.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.lang.Thread;
import java.lang.InterruptedException;

public class BoardStepDefinitions {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String BASE_URL = "http://localhost:8080/kanban";

    @Before
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        // comment the line below to see browser
        options.addArguments("--headless=new");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Given("the Kanban board is open")
    public void the_kanban_board_is_open() {
        driver.get(BASE_URL);
        // Wait until the page body finishes loading
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        
        WebElement projectLink = wait.until(
                ExpectedConditions.elementToBeClickable(By.linkText("Academic Team Project Board"))
        );
        projectLink.click();
    }

    @When("I create a new feature titled {string}")
    public void i_create_a_new_feature_titled(String title) throws InterruptedException {

        WebElement addButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("add-top-level-item-button"))
        );
        addButton.click();
        
        WebElement titleInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.name("name")
        ));
        
        titleInput.clear();
        titleInput.sendKeys(title);

        WebElement avgCaseInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.name("averageCaseEstimate")
        ));
        
        avgCaseInput.clear();
        avgCaseInput.sendKeys("10");

        WebElement wrstCaseInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.name("worstCaseEstimate")
        ));
        
        wrstCaseInput.clear();
        wrstCaseInput.sendKeys("20");

        WebElement importanceInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.name("importance")
        ));
        
        importanceInput.clear();
        importanceInput.sendKeys("100");

        WebElement workStreamInput = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.cssSelector("#workStreamsSelect .tagit-new input, [name='workStreamsSelect'] .tagit-new input")
        ));

        workStreamInput.click();
        workStreamInput.sendKeys("WorkStream1");
        workStreamInput.sendKeys(Keys.ENTER);

        WebElement notesInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.name("notes")
        ));
        
        notesInput.clear();
        notesInput.sendKeys("This is a test feature created for an automated cucumber test");

        WebElement saveButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("save-button"))
        );
        saveButton.click();
    }

    @Then("the feature {string} should appear on the board")
    public void the_feature_should_appear_on_the_board(String title) {

        // Wait for the board to refresh and check if the new title text is rendered
        boolean isPresent = wait.until(driver -> driver.getPageSource().contains(title));
        assertTrue(isPresent, "Expected to find feature '" + title + "' on the Kanban board.");
    }
}
