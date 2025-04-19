package com.ist.leave_management.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateLeaveBalanceDto {
    private Long userId;
    private Long leaveTypeId;
    private Integer balance;
    private LocalDate validUntil;
}