package com.metservice.kanban.kanban.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/** Steps for move_feature_to_wall.feature; the shared Given/When live in {@link CommonBoardSteps}. */
public class MoveFeatureToWallStepDefinitions {

    private final BrowserSession browser;

    public MoveFeatureToWallStepDefinitions(BrowserSession browser) {
        this.browser = browser;
    }

    @When("I move the feature to the wall")
    public void i_move_the_feature_to_the_wall() {
        browser.await()
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector("img.advance")))
                .click();
    }

    @When("I open the wall")
    public void i_open_the_wall() {
        browser.driver().get(BrowserSession.BASE_URL + "/projects/Academic%20Team%20Project%20Board/wall");
        browser.await().until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    @Then("the work item {string} appears on the wall")
    public void the_work_item_appears_on_the_wall(String title) {
        boolean present = browser.await().until(d -> d.getPageSource().contains(title));
        assertTrue(present, "Expected feature '" + title + "' to appear on the wall.");
    }
}
