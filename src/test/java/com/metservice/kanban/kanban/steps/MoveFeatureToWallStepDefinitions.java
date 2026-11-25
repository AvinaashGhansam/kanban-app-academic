package com.metservice.kanban.kanban.steps;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class MoveFeatureToWallStepDefinitions {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String BASE_URL =
        "http://localhost:8080/kanban";

    @Before
    public void setUp() {

        ChromeOptions options = new ChromeOptions();

        // Comment this out if you want to watch the browser
        options.addArguments("--headless=new");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
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

        wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.tagName("body")));

        WebElement projectLink = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.linkText("Academic Team Project Board")));

        projectLink.click();
    }

    @When("I create a new feature titled {string}")
    public void i_create_a_new_feature_titled(String title) {

        WebElement addButton = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.id("add-top-level-item-button")));

        addButton.click();

        WebElement titleInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.name("name")));

        titleInput.clear();
        titleInput.sendKeys(title);

        driver.findElement(
            By.name("averageCaseEstimate"))
            .sendKeys("10");

        driver.findElement(
            By.name("worstCaseEstimate"))
            .sendKeys("20");

        driver.findElement(
            By.name("importance"))
            .sendKeys("100");

        WebElement workStreamInput = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.cssSelector(
                    "#workStreamsSelect .tagit-new input, " +
                    "[name='workStreamsSelect'] .tagit-new input")));

        workStreamInput.sendKeys("WorkStream1");
        workStreamInput.sendKeys(Keys.ENTER);

        driver.findElement(
            By.name("notes"))
            .sendKeys("Feature created by automated test");

        driver.findElement(
            By.id("save-button"))
            .click();

        wait.until(
            d -> d.getPageSource().contains(title));
    }

    @When("I move the feature to the wall")
    public void i_move_the_feature_to_the_wall() {

        WebElement advanceButton = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.cssSelector("img.advance")));

        advanceButton.click();
    }

    @When("I open the wall")
    public void i_open_the_wall() {

        driver.get(
            "http://localhost:8080/kanban/projects/Academic%20Team%20Project%20Board/wall");

        wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.tagName("body")));
    }

    @Then("the work item {string} appears on the wall")
    public void the_work_item_appears_on_the_wall(String title) {

        boolean present = wait.until(
            d -> d.getPageSource().contains(title));

        assertTrue(
            present,
            "Expected feature '" + title + "' to appear on the wall.");
    }
}