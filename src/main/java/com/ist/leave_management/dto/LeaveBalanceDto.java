package com.ist.leave_management.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveBalanceDto {
    private Long id;
    private UserDto user;
    private LeaveTypeDto leaveType;
    private Integer balance;
    private LocalDate validUntil;
    private LocalDate lastAccrualDate;
    private Integer carriedForwardDays;
    private Integer expiredDays;
    private Integer yearlyAccruedDays;
}

@Data
class UserDto {
    private Long id;
    private String username;
    private String email;
}

@Data
class LeaveTypeDto {
    private Long id;
    private String name;
    private String description;
    private Integer maxDays;
    private Boolean isPaid;
    private Boolean requiresApproval;
}