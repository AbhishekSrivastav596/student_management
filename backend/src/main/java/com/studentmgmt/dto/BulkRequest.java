package com.studentmgmt.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkRequest {
    @NotEmpty(message = "At least one student ID is required")
    private List<Long> ids;
}
