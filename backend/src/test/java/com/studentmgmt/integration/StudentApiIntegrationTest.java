package com.studentmgmt.integration;

import com.studentmgmt.entity.Student;
import com.studentmgmt.repository.StudentRepository;
import com.studentmgmt.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StudentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        studentRepository.deleteAll();

        // Register a user and get JWT token
        userRepository.deleteAll();
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Admin\",\"email\":\"admin@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = registerResult.getResponse().getContentAsString();
        // Extract token from JSON response
        jwtToken = body.split("\"token\":\"")[1].split("\"")[0];
    }

    @Nested
    @DisplayName("GET /api/students/stats")
    class StatsEndpoint {

        @Test
        @DisplayName("should return correct stats with mixed active/inactive students")
        void shouldReturnCorrectStats() throws Exception {
            // Seed data
            studentRepository.saveAll(List.of(
                    Student.builder().firstName("A").lastName("One").email("a@test.com").active(true).build(),
                    Student.builder().firstName("B").lastName("Two").email("b@test.com").active(true).build(),
                    Student.builder().firstName("C").lastName("Three").email("c@test.com").active(false).build()
            ));

            mockMvc.perform(get("/api/students/stats")
                            .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(3))
                    .andExpect(jsonPath("$.active").value(2))
                    .andExpect(jsonPath("$.inactive").value(1));
        }

        @Test
        @DisplayName("should return all zeros when no students exist")
        void shouldReturnZerosWhenEmpty() throws Exception {
            mockMvc.perform(get("/api/students/stats")
                            .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.active").value(0))
                    .andExpect(jsonPath("$.inactive").value(0));
        }

        @Test
        @DisplayName("should return 403 without auth token")
        void shouldRejectWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/students/stats"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/students/export/csv")
    class ExportCsvEndpoint {

        @Test
        @DisplayName("should export all students as CSV")
        void shouldExportAllStudents() throws Exception {
            studentRepository.saveAll(List.of(
                    Student.builder().firstName("John").lastName("Doe").email("john@test.com")
                            .phone("123").studentClass("10").section("A")
                            .enrollmentDate(LocalDate.of(2024, 1, 15)).active(true).build(),
                    Student.builder().firstName("Jane").lastName("Smith").email("jane@test.com")
                            .active(false).build()
            ));

            MvcResult result = mockMvc.perform(get("/api/students/export/csv")
                            .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=students.csv"))
                    .andReturn();

            String csv = result.getResponse().getContentAsString();
            String[] lines = csv.trim().split("\n");
            assertThat(lines).hasSize(3); // header + 2 students
            assertThat(lines[0].trim()).isEqualTo("firstName,lastName,email,phone,class,section,enrollmentDate,active");
            assertThat(csv).contains("John", "Doe", "john@test.com");
            assertThat(csv).contains("Jane", "Smith", "jane@test.com");
        }

        @Test
        @DisplayName("should filter by active status when param provided")
        void shouldFilterByActive() throws Exception {
            studentRepository.saveAll(List.of(
                    Student.builder().firstName("Active").lastName("One").email("active@test.com").active(true).build(),
                    Student.builder().firstName("Inactive").lastName("Two").email("inactive@test.com").active(false).build()
            ));

            MvcResult result = mockMvc.perform(get("/api/students/export/csv")
                            .param("active", "true")
                            .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andReturn();

            String csv = result.getResponse().getContentAsString();
            assertThat(csv).contains("Active");
            assertThat(csv).doesNotContain("Inactive");
        }
    }

    @Nested
    @DisplayName("POST /api/students/import/csv")
    class ImportCsvEndpoint {

        @Test
        @DisplayName("should import valid CSV file")
        void shouldImportValidCsv() throws Exception {
            String csvContent = "firstName,lastName,email,phone,class,section,enrollmentDate,active\n"
                    + "Alice,Wonder,alice@test.com,555-0100,9,B,2024-03-01,true\n"
                    + "Bob,Builder,bob@test.com,555-0200,10,A,,false\n";

            MockMultipartFile file = new MockMultipartFile(
                    "file", "students.csv", "text/csv", csvContent.getBytes());

            mockMvc.perform(multipart("/api/students/import/csv")
                            .file(file)
                            .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.imported").value(2))
                    .andExpect(jsonPath("$.failed").value(0));

            List<Student> students = studentRepository.findAll();
            assertThat(students).hasSize(2);
            assertThat(students).extracting(Student::getEmail)
                    .containsExactlyInAnyOrder("alice@test.com", "bob@test.com");
        }

        @Test
        @DisplayName("should report errors for malformed rows")
        void shouldReportErrors() throws Exception {
            String csvContent = "firstName,lastName,email\n"
                    + ",Missing,\n"  // missing firstName and email
                    + "Valid,User,valid@test.com\n";

            MockMultipartFile file = new MockMultipartFile(
                    "file", "students.csv", "text/csv", csvContent.getBytes());

            mockMvc.perform(multipart("/api/students/import/csv")
                            .file(file)
                            .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.imported").value(1))
                    .andExpect(jsonPath("$.failed").value(1))
                    .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("Row 2")));

            assertThat(studentRepository.findAll()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Repository: countByActive")
    class RepositoryTests {

        @Test
        @DisplayName("should count active and inactive students correctly")
        void shouldCountByActiveStatus() {
            studentRepository.saveAll(List.of(
                    Student.builder().firstName("A").lastName("X").email("a@r.com").active(true).build(),
                    Student.builder().firstName("B").lastName("Y").email("b@r.com").active(true).build(),
                    Student.builder().firstName("C").lastName("Z").email("c@r.com").active(true).build(),
                    Student.builder().firstName("D").lastName("W").email("d@r.com").active(false).build()
            ));

            assertThat(studentRepository.countByActive(true)).isEqualTo(3);
            assertThat(studentRepository.countByActive(false)).isEqualTo(1);
            assertThat(studentRepository.count()).isEqualTo(4);
        }
    }
}
