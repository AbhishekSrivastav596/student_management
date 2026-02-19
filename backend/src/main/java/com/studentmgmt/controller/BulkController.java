package com.studentmgmt.controller;

import com.studentmgmt.dto.BulkRequest;
import com.studentmgmt.dto.StudentDto;
import com.studentmgmt.service.EmailService;
import com.studentmgmt.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students/bulk")
@RequiredArgsConstructor
public class BulkController {

    private final StudentService studentService;
    private final EmailService emailService;

    @PostMapping("/delete")
    public ResponseEntity<Map<String, String>> bulkDelete(@Valid @RequestBody BulkRequest request) {
        studentService.bulkDelete(request.getIds());
        return ResponseEntity.ok(Map.of("message", "Deleted " + request.getIds().size() + " students"));
    }

    @PostMapping("/activate")
    public ResponseEntity<Map<String, String>> bulkActivate(@Valid @RequestBody BulkRequest request) {
        studentService.bulkSetActive(request.getIds(), true);
        return ResponseEntity.ok(Map.of("message", "Activated " + request.getIds().size() + " students"));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, String>> bulkDeactivate(@Valid @RequestBody BulkRequest request) {
        studentService.bulkSetActive(request.getIds(), false);
        return ResponseEntity.ok(Map.of("message", "Deactivated " + request.getIds().size() + " students"));
    }

    @PostMapping("/send-invite")
    public ResponseEntity<Map<String, Object>> bulkSendInvite(@Valid @RequestBody BulkRequest request) {
        List<StudentDto> students = studentService.getByIds(request.getIds());

        // Filter only active students
        List<StudentDto> activeStudents = students.stream()
                .filter(StudentDto::isActive)
                .toList();

        if (activeStudents.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No active students in selection",
                    "sent", 0,
                    "failed", 0
            ));
        }

        EmailService.BulkInviteResult result = emailService.sendBulkInvites(activeStudents);
        return ResponseEntity.ok(Map.of(
                "message", "Invites sent",
                "sent", result.sent(),
                "failed", result.failed(),
                "errors", result.errors()
        ));
    }
}
