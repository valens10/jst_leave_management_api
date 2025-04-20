package com.ist.leave_management.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
public class UpdateLeaveTypeDto {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Maximum days is required")
    @Positive(message = "Maximum days must be positive")
    private Integer maxDays;

    @NotNull(message = "Is paid status is required")
    private Boolean isPaid;

    @NotNull(message = "Requires approval status is required")
    private Boolean requiresApproval;

    @NotNull(message = "Is annual leave status is required")
    private Boolean isAnnualLeave;
}