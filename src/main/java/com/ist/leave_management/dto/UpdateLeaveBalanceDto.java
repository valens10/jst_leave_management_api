package com.ist.leave_management.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateLeaveBalanceDto {
    private Integer balance;
    private LocalDate validUntil;
}