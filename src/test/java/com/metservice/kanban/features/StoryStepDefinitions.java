package com.metservice.kanban.features;

import static org.assertj.core.api.Assertions.assertThat;

import com.metservice.kanban.model.WorkItem;
import com.metservice.kanban.model.WorkItemType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Arrays;
import org.joda.time.LocalDate;

public class StoryStepDefinitions {
    // Can be fixed here
    private static final LocalDate ANY_DATE = new LocalDate(2026, 1, 1);

    private WorkItemType storyType;
    private WorkItem story;
    private Exception thrown;

    @Given("a story work item type with phases {string}")
    public void aStoryWorkItemTypeWithPhases(String phases) {
        storyType = new WorkItemType(
                Arrays.stream(phases.split(",")).map(String::trim).toArray(String[]::new));
        storyType.setName("story");
    }

    /** Mirrors {@code DefaultKanbanProject.addWorkItem}: construct the item, then advance it onto the board. */
    @Given("a new story work item of that type")
    public void aNewStoryWorkItemOfThatType() {
        story = new WorkItem(1, storyType);
        story.advance(ANY_DATE);
    }

    /** Walks the story forward with {@code advance} until it reaches the requested phase. */
    @Given("the story is in phase {string}")
    public void theStoryIsInPhase(String phase) {
        assertThat(storyType.getPhases())
                .as("phase must be declared on the type")
                .contains(phase);
        while (!story.getCurrentPhase().equals(phase)) {
            story.advance(ANY_DATE);
        }
    }

    @When("I advance the story")
    public void iAdvanceTheStory() {
        story.advance(ANY_DATE);
    }

    @When("I try to advance the story")
    public void iTryToAdvanceTheStory() {
        try {
            story.advance(ANY_DATE);
        } catch (Exception e) {
            thrown = e;
        }
    }

    @Then("the story's current phase is {string}")
    @Then("the story's current phase is still {string}")
    public void theStorysCurrentPhaseIs(String expectedPhase) {
        assertThat(story.getCurrentPhase()).isEqualTo(expectedPhase);
    }

    @Then("advancing fails with an IllegalStateException")
    public void advancingFailsWithAnIllegalStateException() {
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Then("the story is completed")
    public void theStoryIsCompleted() {
        assertThat(story.isCompleted()).isTrue();
    }
}
