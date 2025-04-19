package com.ist.leave_management.dto;

import com.ist.leave_management.model.LeaveWorkflowStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LeaveWorkflowDto {
    private Long id;
    private LeaveApplicationResponseDto leaveApplication;
    private Long approvedById;
    private String approvedByName;
    private Long rejectedById;
    private String rejectedByName;
    private LocalDateTime approvedDate;
    private LocalDateTime rejectedDate;
    private String comments;
    private LeaveWorkflowStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}