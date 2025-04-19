package com.ist.leave_management.dto;

import com.ist.leave_management.model.LeaveStatus;
import com.ist.leave_management.model.LeaveType;
import com.ist.user_management.model.User;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LeaveApplicationResponseDto {
    private Long id;
    private User user;
    private Long leaveTypeId;
    private String leaveTypeName;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isHalfDay;
    private String attachment;
    private LeaveStatus status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}