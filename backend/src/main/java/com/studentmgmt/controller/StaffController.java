package com.studentmgmt.controller;

import com.studentmgmt.dto.StaffDto;
import com.studentmgmt.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public ResponseEntity<Page<StaffDto>> getAll(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(required = false) Boolean active) {

        Sort sort = order.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return ResponseEntity.ok(staffService.getAll(search, active, PageRequest.of(page, size, sort)));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<StaffDto> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.toggleActive(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.getById(id));
    }

    @PostMapping
    public ResponseEntity<StaffDto> create(@Valid @RequestBody StaffDto dto) {
        return ResponseEntity.ok(staffService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffDto> update(@PathVariable Long id, @Valid @RequestBody StaffDto dto) {
        return ResponseEntity.ok(staffService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        staffService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
