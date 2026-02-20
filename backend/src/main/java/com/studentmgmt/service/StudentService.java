package com.studentmgmt.service;

import com.studentmgmt.dto.StudentDto;
import com.studentmgmt.entity.Student;
import com.studentmgmt.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public Page<StudentDto> getAll(String search, Boolean active, Pageable pageable) {
        Page<Student> page;
        boolean hasSearch = search != null && !search.isBlank();

        if (hasSearch && active != null) {
            page = studentRepository.searchByActive(search, active, pageable);
        } else if (hasSearch) {
            page = studentRepository.search(search, pageable);
        } else if (active != null) {
            page = studentRepository.findByActive(active, pageable);
        } else {
            page = studentRepository.findAll(pageable);
        }
        return page.map(this::toDto);
    }

    public StudentDto toggleActive(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        student.setActive(!student.isActive());
        return toDto(studentRepository.save(student));
    }

    public StudentDto getById(Long id) {
        return toDto(studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id)));
    }

    public StudentDto create(StudentDto dto) {
        Student student = toEntity(dto);
        return toDto(studentRepository.save(student));
    }

    public StudentDto update(Long id, StudentDto dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));

        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setEmail(dto.getEmail());
        student.setPhone(dto.getPhone());
        student.setStudentClass(dto.getStudentClass());
        student.setSection(dto.getSection());
        student.setEnrollmentDate(dto.getEnrollmentDate());

        return toDto(studentRepository.save(student));
    }

    public void delete(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new RuntimeException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }

    @Transactional
    public void bulkDelete(List<Long> ids) {
        studentRepository.deleteAllByIdInBatch(ids);
    }

    @Transactional
    public void bulkSetActive(List<Long> ids, boolean active) {
        List<Student> students = studentRepository.findAllByIdIn(ids);
        students.forEach(s -> s.setActive(active));
        studentRepository.saveAll(students);
    }

    public List<StudentDto> getByIds(List<Long> ids) {
        return studentRepository.findAllByIdIn(ids).stream()
                .map(this::toDto)
                .toList();
    }

    private StudentDto toDto(Student student) {
        StudentDto dto = new StudentDto();
        dto.setId(student.getId());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setEmail(student.getEmail());
        dto.setPhone(student.getPhone());
        dto.setStudentClass(student.getStudentClass());
        dto.setSection(student.getSection());
        dto.setEnrollmentDate(student.getEnrollmentDate());
        dto.setActive(student.isActive());
        return dto;
    }

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", studentRepository.count());
        stats.put("active", studentRepository.countByActive(true));
        stats.put("inactive", studentRepository.countByActive(false));
        return stats;
    }

    public void exportCsv(PrintWriter writer, String search, Boolean active) {
        List<Student> students;
        if (search != null && !search.isBlank() && active != null) {
            students = studentRepository.searchByActive(search, active, Pageable.unpaged()).getContent();
        } else if (search != null && !search.isBlank()) {
            students = studentRepository.search(search, Pageable.unpaged()).getContent();
        } else if (active != null) {
            students = studentRepository.findByActive(active, Pageable.unpaged()).getContent();
        } else {
            students = studentRepository.findAll();
        }

        writer.println("firstName,lastName,email,phone,class,section,enrollmentDate,active");
        for (Student s : students) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                    csvEscape(s.getFirstName()),
                    csvEscape(s.getLastName()),
                    csvEscape(s.getEmail()),
                    csvEscape(s.getPhone()),
                    csvEscape(s.getStudentClass()),
                    csvEscape(s.getSection()),
                    s.getEnrollmentDate() != null ? s.getEnrollmentDate().toString() : "",
                    s.isActive());
        }
        writer.flush();
    }

    public Map<String, Object> importCsv(InputStream inputStream) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int row = 1;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String header = reader.readLine(); // skip header
            if (header == null) {
                errors.add("Empty CSV file");
                return Map.of("imported", 0, "failed", 0, "errors", errors);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                try {
                    String[] cols = parseCsvLine(line);
                    if (cols.length < 3) {
                        errors.add("Row " + row + ": need at least firstName, lastName, email");
                        continue;
                    }
                    Student student = Student.builder()
                            .firstName(cols[0].trim())
                            .lastName(cols[1].trim())
                            .email(cols[2].trim())
                            .phone(cols.length > 3 ? cols[3].trim() : null)
                            .studentClass(cols.length > 4 ? cols[4].trim() : null)
                            .section(cols.length > 5 ? cols[5].trim() : null)
                            .enrollmentDate(cols.length > 6 && !cols[6].trim().isEmpty() ? parseDate(cols[6].trim()) : null)
                            .active(cols.length > 7 ? Boolean.parseBoolean(cols[7].trim()) : true)
                            .build();

                    if (student.getFirstName().isEmpty() || student.getEmail().isEmpty()) {
                        errors.add("Row " + row + ": firstName and email are required");
                        continue;
                    }
                    studentRepository.save(student);
                    imported++;
                } catch (Exception e) {
                    errors.add("Row " + row + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("Failed to read CSV: " + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("failed", errors.size());
        result.put("errors", errors);
        return result;
    }

    private LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private Student toEntity(StudentDto dto) {
        return Student.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .studentClass(dto.getStudentClass())
                .section(dto.getSection())
                .enrollmentDate(dto.getEnrollmentDate())
                .active(dto.isActive())
                .build();
    }
}
