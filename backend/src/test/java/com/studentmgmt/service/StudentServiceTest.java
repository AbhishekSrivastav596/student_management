package com.studentmgmt.service;

import com.studentmgmt.dto.StudentDto;
import com.studentmgmt.entity.Student;
import com.studentmgmt.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    @Nested
    @DisplayName("getStats()")
    class GetStats {

        @Test
        @DisplayName("should return total, active, and inactive counts")
        void shouldReturnCorrectCounts() {
            when(studentRepository.count()).thenReturn(10L);
            when(studentRepository.countByActive(true)).thenReturn(7L);
            when(studentRepository.countByActive(false)).thenReturn(3L);

            Map<String, Long> stats = studentService.getStats();

            assertThat(stats).containsEntry("total", 10L);
            assertThat(stats).containsEntry("active", 7L);
            assertThat(stats).containsEntry("inactive", 3L);
            verify(studentRepository).count();
            verify(studentRepository).countByActive(true);
            verify(studentRepository).countByActive(false);
        }

        @Test
        @DisplayName("should return zeros when no students exist")
        void shouldReturnZerosWhenEmpty() {
            when(studentRepository.count()).thenReturn(0L);
            when(studentRepository.countByActive(true)).thenReturn(0L);
            when(studentRepository.countByActive(false)).thenReturn(0L);

            Map<String, Long> stats = studentService.getStats();

            assertThat(stats.get("total")).isZero();
            assertThat(stats.get("active")).isZero();
            assertThat(stats.get("inactive")).isZero();
        }
    }

    @Nested
    @DisplayName("exportCsv()")
    class ExportCsv {

        @Test
        @DisplayName("should write CSV header and student rows")
        void shouldWriteCsvWithStudents() {
            Student student = Student.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .phone("1234567890")
                    .studentClass("10")
                    .section("A")
                    .enrollmentDate(LocalDate.of(2024, 1, 15))
                    .active(true)
                    .build();

            when(studentRepository.findAll()).thenReturn(List.of(student));

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            studentService.exportCsv(pw, null, null);

            String csv = sw.toString();
            String[] lines = csv.split("\n");
            assertThat(lines).hasSize(2);
            assertThat(lines[0].trim()).isEqualTo("firstName,lastName,email,phone,class,section,enrollmentDate,active");
            assertThat(lines[1]).contains("John", "Doe", "john@example.com", "1234567890", "10", "A", "2024-01-15", "true");
        }

        @Test
        @DisplayName("should handle null fields gracefully")
        void shouldHandleNullFields() {
            Student student = Student.builder()
                    .firstName("Jane")
                    .lastName("Doe")
                    .email("jane@example.com")
                    .active(false)
                    .build();

            when(studentRepository.findAll()).thenReturn(List.of(student));

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            studentService.exportCsv(pw, null, null);

            String csv = sw.toString();
            assertThat(csv).contains("Jane,Doe,jane@example.com,,,,");
        }

        @Test
        @DisplayName("should escape commas in field values")
        void shouldEscapeCommasInFields() {
            Student student = Student.builder()
                    .firstName("John")
                    .lastName("Doe, Jr.")
                    .email("john@example.com")
                    .active(true)
                    .build();

            when(studentRepository.findAll()).thenReturn(List.of(student));

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            studentService.exportCsv(pw, null, null);

            String csv = sw.toString();
            assertThat(csv).contains("\"Doe, Jr.\"");
        }
    }

    @Nested
    @DisplayName("importCsv()")
    class ImportCsv {

        @Test
        @DisplayName("should import valid CSV rows")
        void shouldImportValidRows() {
            String csv = "firstName,lastName,email,phone,class,section,enrollmentDate,active\n"
                    + "John,Doe,john@example.com,1234567890,10,A,2024-01-15,true\n"
                    + "Jane,Smith,jane@example.com,,,,,false\n";

            when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = studentService.importCsv(
                    new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

            assertThat(result.get("imported")).isEqualTo(2);
            assertThat(result.get("failed")).isEqualTo(0);
            verify(studentRepository, times(2)).save(any(Student.class));
        }

        @Test
        @DisplayName("should report errors for invalid rows")
        void shouldReportErrorsForInvalidRows() {
            String csv = "firstName,lastName,email\n"
                    + ",Doe,\n"  // missing firstName and email
                    + "John,Doe,john@example.com\n";

            when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = studentService.importCsv(
                    new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

            assertThat(result.get("imported")).isEqualTo(1);
            assertThat(result.get("failed")).isEqualTo(1);
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) result.get("errors");
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0)).contains("Row 2");
        }

        @Test
        @DisplayName("should handle empty CSV file")
        void shouldHandleEmptyCsv() {
            String csv = "";

            Map<String, Object> result = studentService.importCsv(
                    new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

            assertThat(result.get("imported")).isEqualTo(0);
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) result.get("errors");
            assertThat(errors).contains("Empty CSV file");
        }

        @Test
        @DisplayName("should handle CSV with only header row")
        void shouldHandleHeaderOnlyCsv() {
            String csv = "firstName,lastName,email\n";

            Map<String, Object> result = studentService.importCsv(
                    new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

            assertThat(result.get("imported")).isEqualTo(0);
            assertThat(result.get("failed")).isEqualTo(0);
            verify(studentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should parse quoted fields with commas")
        void shouldParseQuotedFieldsWithCommas() {
            String csv = "firstName,lastName,email\n"
                    + "John,\"Doe, Jr.\",john@example.com\n";

            when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
                Student s = inv.getArgument(0);
                assertThat(s.getLastName()).isEqualTo("Doe, Jr.");
                return s;
            });

            Map<String, Object> result = studentService.importCsv(
                    new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

            assertThat(result.get("imported")).isEqualTo(1);
        }
    }
}
