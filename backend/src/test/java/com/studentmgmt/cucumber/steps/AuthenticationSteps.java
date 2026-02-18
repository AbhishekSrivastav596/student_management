package com.studentmgmt.cucumber.steps;

import com.studentmgmt.repository.UserRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthenticationSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private ResultActions result;

    @Before
    public void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Given("no account exists for {string}")
    public void noAccountExistsFor(String email) {
        // Database is clean after the @Before hook — this step documents the precondition
    }

    @Given("a registered user with name {string}, email {string}, and password {string}")
    public void aRegisteredUserWithNameEmailAndPassword(String name, String email, String password) throws Exception {
        String json = String.format(
                "{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                name, email, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @When("a user registers with name {string}, email {string}, and password {string}")
    public void aUserRegistersWithNameEmailAndPassword(String name, String email, String password) throws Exception {
        String json = String.format(
                "{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                name, email, password);
        result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @When("the user logs in with email {string} and password {string}")
    public void theUserLogsInWithEmailAndPassword(String email, String password) throws Exception {
        String json = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Then("the response status code is {int}")
    public void theResponseStatusCodeIs(int statusCode) throws Exception {
        result.andExpect(status().is(statusCode));
    }

    @Then("the response body contains a JWT token")
    public void theResponseBodyContainsAJwtToken() throws Exception {
        result.andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Then("the response body contains the name {string}")
    public void theResponseBodyContainsTheName(String name) throws Exception {
        result.andExpect(jsonPath("$.name").value(name));
    }
}
