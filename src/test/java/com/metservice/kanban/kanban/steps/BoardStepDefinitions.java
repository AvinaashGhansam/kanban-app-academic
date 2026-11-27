package com.metservice.kanban.kanban.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.java.en.Then;

/** Steps for create_feature.feature; the shared Given/When live in {@link CommonBoardSteps}. */
public class BoardStepDefinitions {

    private final BrowserSession browser;

    public BoardStepDefinitions(BrowserSession browser) {
        this.browser = browser;
    }

    @Then("the feature {string} should appear on the board")
    public void the_feature_should_appear_on_the_board(String title) {
        // Wait for the board to refresh and check if the new title text is rendered
        boolean isPresent = browser.await().until(driver -> driver.getPageSource().contains(title));
        assertTrue(isPresent, "Expected to find feature '" + title + "' on the Kanban board.");
    }
}
