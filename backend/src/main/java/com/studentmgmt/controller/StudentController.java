package com.studentmgmt.controller;

import com.studentmgmt.dto.StudentDto;
import com.studentmgmt.service.StudentService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.PrintWriter;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public ResponseEntity<Page<StudentDto>> getAll(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(required = false) Boolean active) {

        Sort sort = order.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return ResponseEntity.ok(studentService.getAll(search, active, PageRequest.of(page, size, sort)));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<StudentDto> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.toggleActive(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getById(id));
    }

    @PostMapping
    public ResponseEntity<StudentDto> create(@Valid @RequestBody StudentDto dto) {
        return ResponseEntity.ok(studentService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDto> update(@PathVariable Long id, @Valid @RequestBody StudentDto dto) {
        return ResponseEntity.ok(studentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(studentService.getStats());
    }

    @GetMapping("/export/csv")
    public void exportCsv(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Boolean active,
            HttpServletResponse response) throws Exception {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=students.csv");
        PrintWriter writer = response.getWriter();
        studentService.exportCsv(writer, search.isBlank() ? null : search, active);
    }

    @PostMapping("/import/csv")
    public ResponseEntity<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(studentService.importCsv(file.getInputStream()));
    }
}
