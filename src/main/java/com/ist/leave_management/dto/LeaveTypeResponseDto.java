package com.ist.leave_management.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LeaveTypeResponseDto {
    private Long id;
    private String name;
    private String description;
    private Integer maxDays;
    private Boolean isPaid;
    private Boolean requiresApproval;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}