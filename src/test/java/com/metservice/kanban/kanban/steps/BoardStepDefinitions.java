package com.metservice.kanban.kanban.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class BoardStepDefinitions {

    // Inject your WebDriver or Kanban backend service/DAO here

    @Given("the Kanban board is open")
    public void the_kanban_board_is_open() {
        // Navigate to http://localhost:8080/kanban-app (or your configured context path)
    }

    @When("I create a new feature titled {string}")
    public void i_create_a_new_feature_titled(String title) {
        // Click the "Add" button, enter `title` into the input field, and submit
    }

    @Then("the feature {string} should appear on the board")
    public void the_feature_should_appear_on_the_board(String title) {
        // Verify the page source or initial column contains `title`
        boolean featureExists = true; // Replace with WebDriver element check
        assertTrue(featureExists, "Feature not found on board: " + title);
    }
}
