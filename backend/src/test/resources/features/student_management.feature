Feature: Student CSV Import/Export and Statistics
  As an administrator
  I want to import/export students via CSV and view statistics
  So that I can efficiently manage student records in bulk

  Background:
    Given a registered admin with name "Admin", email "admin@school.com", and password "admin123"

  Scenario: View student statistics with mixed active and inactive students
    Given the following students exist:
      | firstName | lastName | email            | active |
      | Alice     | Wonder   | alice@school.com | true   |
      | Bob       | Builder  | bob@school.com   | true   |
      | Charlie   | Brown    | charlie@school.com | false |
    When the admin requests student statistics
    Then the stats response status is 200
    And the total count is 3
    And the active count is 2
    And the inactive count is 1

  Scenario: Export students to CSV and verify format
    Given the following students exist:
      | firstName | lastName | email            | active |
      | Alice     | Wonder   | alice@school.com | true   |
      | Bob       | Builder  | bob@school.com   | false  |
    When the admin exports students as CSV
    Then the export response status is 200
    And the CSV contains a header row "firstName,lastName,email,phone,class,section,enrollmentDate,active"
    And the CSV contains 2 data rows

  Scenario: Import students from CSV file
    When the admin imports a CSV file with content:
      """
      firstName,lastName,email,phone,class,section,enrollmentDate,active
      Diana,Prince,diana@school.com,555-0001,10,A,2024-06-01,true
      Clark,Kent,clark@school.com,555-0002,11,B,,true
      """
    Then the import response status is 200
    And the import result shows 2 imported and 0 failed
    And the student "diana@school.com" exists in the system
    And the student "clark@school.com" exists in the system
