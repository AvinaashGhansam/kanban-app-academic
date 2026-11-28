package com.metservice.kanban.kanban.steps;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Steps used by more than one browser feature, plus the browser teardown hook. */
public class CommonBoardSteps {

    private final BrowserSession browser;

    public CommonBoardSteps(BrowserSession browser) {
        this.browser = browser;
    }

    @After
    public void tearDown() {
        browser.quit();
    }

    @Given("the Kanban board is open")
    public void the_kanban_board_is_open() {
        WebDriverWait wait = browser.await();
        browser.driver().get(BrowserSession.BASE_URL);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Academic Team Project Board")))
                .click();
    }

    @When("I create a new feature titled {string}")
    public void i_create_a_new_feature_titled(String title) {
        WebDriverWait wait = browser.await();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("add-top-level-item-button")))
                .click();

        fill(wait, "name", title);
        fill(wait, "averageCaseEstimate", "10");
        fill(wait, "worstCaseEstimate", "20");
        fill(wait, "importance", "100");

        WebElement workStreamInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("#workStreamsSelect .tagit-new input, [name='workStreamsSelect'] .tagit-new input")));
        workStreamInput.click();
        workStreamInput.sendKeys("WorkStream1");
        workStreamInput.sendKeys(Keys.ENTER);

        fill(wait, "notes", "Feature created by an automated cucumber test");

        wait.until(ExpectedConditions.elementToBeClickable(By.id("save-button")))
                .click();
        wait.until(d -> d.getPageSource().contains(title));
    }

    private static void fill(WebDriverWait wait, String fieldName, String value) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name(fieldName)));
        input.clear();
        input.sendKeys(value);
    }
}
