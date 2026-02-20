package com.studentmgmt.cucumber.steps;

import com.studentmgmt.entity.Student;
import com.studentmgmt.repository.StudentRepository;
import com.studentmgmt.repository.UserRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class StudentManagementSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    private String jwtToken;
    private ResultActions statsResult;
    private MvcResult exportResult;
    private ResultActions importResult;

    @Before
    public void cleanDatabase() {
        studentRepository.deleteAll();
    }

    @Given("a registered admin with name {string}, email {string}, and password {string}")
    public void aRegisteredAdmin(String name, String email, String password) throws Exception {
        userRepository.deleteAll();
        String json = String.format(
                "{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                name, email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        jwtToken = body.split("\"token\":\"")[1].split("\"")[0];
    }

    @Given("the following students exist:")
    public void theFollowingStudentsExist(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            studentRepository.save(Student.builder()
                    .firstName(row.get("firstName"))
                    .lastName(row.get("lastName"))
                    .email(row.get("email"))
                    .active(Boolean.parseBoolean(row.get("active")))
                    .build());
        }
    }

    // --- Stats ---

    @When("the admin requests student statistics")
    public void theAdminRequestsStudentStatistics() throws Exception {
        statsResult = mockMvc.perform(get("/api/students/stats")
                .header("Authorization", "Bearer " + jwtToken));
    }

    @Then("the stats response status is {int}")
    public void theStatsResponseStatusIs(int statusCode) throws Exception {
        statsResult.andExpect(status().is(statusCode));
    }

    @And("the total count is {int}")
    public void theTotalCountIs(int count) throws Exception {
        statsResult.andExpect(jsonPath("$.total").value(count));
    }

    @And("the active count is {int}")
    public void theActiveCountIs(int count) throws Exception {
        statsResult.andExpect(jsonPath("$.active").value(count));
    }

    @And("the inactive count is {int}")
    public void theInactiveCountIs(int count) throws Exception {
        statsResult.andExpect(jsonPath("$.inactive").value(count));
    }

    // --- Export ---

    @When("the admin exports students as CSV")
    public void theAdminExportsStudentsAsCsv() throws Exception {
        exportResult = mockMvc.perform(get("/api/students/export/csv")
                        .header("Authorization", "Bearer " + jwtToken))
                .andReturn();
    }

    @Then("the export response status is {int}")
    public void theExportResponseStatusIs(int statusCode) {
        assertThat(exportResult.getResponse().getStatus()).isEqualTo(statusCode);
    }

    @And("the CSV contains a header row {string}")
    public void theCsvContainsHeaderRow(String expectedHeader) throws UnsupportedEncodingException {
        String csv = exportResult.getResponse().getContentAsString();
        String firstLine = csv.split("\n")[0].trim();
        assertThat(firstLine).isEqualTo(expectedHeader);
    }

    @And("the CSV contains {int} data rows")
    public void theCsvContainsDataRows(int count) throws UnsupportedEncodingException {
        String csv = exportResult.getResponse().getContentAsString();
        String[] lines = csv.trim().split("\n");
        assertThat(lines.length - 1).isEqualTo(count); // minus header
    }

    // --- Import ---

    @When("the admin imports a CSV file with content:")
    public void theAdminImportsCsvFileWithContent(String csvContent) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "import.csv", "text/csv", csvContent.getBytes());

        importResult = mockMvc.perform(multipart("/api/students/import/csv")
                .file(file)
                .header("Authorization", "Bearer " + jwtToken));
    }

    @Then("the import response status is {int}")
    public void theImportResponseStatusIs(int statusCode) throws Exception {
        importResult.andExpect(status().is(statusCode));
    }

    @And("the import result shows {int} imported and {int} failed")
    public void theImportResultShows(int imported, int failed) throws Exception {
        importResult.andExpect(jsonPath("$.imported").value(imported));
        importResult.andExpect(jsonPath("$.failed").value(failed));
    }

    @And("the student {string} exists in the system")
    public void theStudentExistsInTheSystem(String email) {
        Optional<Student> student = studentRepository.findAll().stream()
                .filter(s -> s.getEmail().equals(email))
                .findFirst();
        assertThat(student).isPresent();
    }
}
