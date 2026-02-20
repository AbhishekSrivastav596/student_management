package com.studentmgmt.controller;

import com.studentmgmt.dto.BulkRequest;
import com.studentmgmt.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/staff/bulk")
@RequiredArgsConstructor
public class StaffBulkController {

    private final StaffService staffService;

    @PostMapping("/delete")
    public ResponseEntity<Map<String, String>> bulkDelete(@Valid @RequestBody BulkRequest request) {
        staffService.bulkDelete(request.getIds());
        return ResponseEntity.ok(Map.of("message", "Deleted " + request.getIds().size() + " staff members"));
    }

    @PostMapping("/activate")
    public ResponseEntity<Map<String, String>> bulkActivate(@Valid @RequestBody BulkRequest request) {
        staffService.bulkSetActive(request.getIds(), true);
        return ResponseEntity.ok(Map.of("message", "Activated " + request.getIds().size() + " staff members"));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, String>> bulkDeactivate(@Valid @RequestBody BulkRequest request) {
        staffService.bulkSetActive(request.getIds(), false);
        return ResponseEntity.ok(Map.of("message", "Deactivated " + request.getIds().size() + " staff members"));
    }
}
