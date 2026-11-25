Feature: Move feature to wall

  Scenario: Move a feature from backlog to wall

    Given the Kanban board is open
    When I create a new feature titled "Wall Test Feature"
    And I move the feature to the wall
    And I open the wall
    Then the work item "Wall Test Feature" appears on the wall
