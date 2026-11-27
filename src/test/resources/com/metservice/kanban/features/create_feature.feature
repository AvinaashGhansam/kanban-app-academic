@ui
Feature: Kanban Board Management
  As a project team member
  I want to add a new feature to the Kanban board
  So that I can visualize and track progress across workflow stages

  Scenario: Add a new feature to the board
    Given the Kanban board is open
    When I create a new feature titled "Cucumber acceptance tests"
    Then the feature "Cucumber acceptance tests" should appear on the board
