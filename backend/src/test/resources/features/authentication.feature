Feature: User Authentication
  As a system user
  I want to register and log in to the application
  So that I can securely manage student records

  Scenario: Successful user registration with valid details
    Given no account exists for "newuser@example.com"
    When a user registers with name "New User", email "newuser@example.com", and password "secret123"
    Then the response status code is 200
    And the response body contains a JWT token
    And the response body contains the name "New User"

  Scenario: Successful login with valid credentials
    Given a registered user with name "Login User", email "login@example.com", and password "pass123"
    When the user logs in with email "login@example.com" and password "pass123"
    Then the response status code is 200
    And the response body contains a JWT token

  Scenario: Login fails when credentials are incorrect
    When the user logs in with email "ghost@example.com" and password "wrongpassword"
    Then the response status code is 401
