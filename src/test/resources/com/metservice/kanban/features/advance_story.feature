Feature: Advancing a story through its phases
  As a project team member
  I want to advance a story to its next workflow phase
  So that the Kanban board reflects the story's true progress

  Background:
    Given a story work item type with phases "Story, Development, Completed Story"
    And a new story work item of that type

  Scenario: A new story starts in its first phase
    Then the story's current phase is "Story"

  Scenario Outline: Advancing moves the story to the next phase
    Given the story is in phase "<from>"
    When I advance the story
    Then the story's current phase is "<to>"

    Examples:
      | from        | to              |
      | Story       | Development     |
      | Development | Completed Story |

  Scenario: Advancing past the last phase is rejected
    Given the story is in phase "Completed Story"
    When I try to advance the story
    Then advancing fails with an IllegalStateException
    And the story's current phase is still "Completed Story"

  Scenario: A story in the last phase is completed
    Given the story is in phase "Completed Story"
    Then the story is completed
