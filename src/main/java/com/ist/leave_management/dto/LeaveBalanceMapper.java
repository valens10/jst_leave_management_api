package com.ist.leave_management.dto;

import com.ist.leave_management.model.LeaveBalance;
import org.springframework.stereotype.Component;

@Component
public class LeaveBalanceMapper {
    public LeaveBalance toEntity(CreateLeaveBalanceDto dto) {
        LeaveBalance leaveBalance = new LeaveBalance();
        leaveBalance.setBalance(dto.getBalance());
        leaveBalance.setValidUntil(dto.getValidUntil());
        return leaveBalance;
    }

    public LeaveBalance toEntity(UpdateLeaveBalanceDto dto, LeaveBalance leaveBalance) {
        leaveBalance.setBalance(dto.getBalance());
        leaveBalance.setValidUntil(dto.getValidUntil());
        return leaveBalance;
    }

    public LeaveBalanceDto toDto(LeaveBalance leaveBalance) {
        LeaveBalanceDto dto = new LeaveBalanceDto();
        dto.setId(leaveBalance.getId());

        // Map user
        UserDto userDto = new UserDto();
        userDto.setId(leaveBalance.getUser().getId());
        userDto.setEmail(leaveBalance.getUser().getEmail());
        userDto.setUsername(leaveBalance.getUser().getFirstName() + " " + leaveBalance.getUser().getLastName());
        dto.setUser(userDto);

        // Map leave type
        LeaveTypeDto leaveTypeDto = new LeaveTypeDto();
        leaveTypeDto.setId(leaveBalance.getLeaveType().getId());
        leaveTypeDto.setName(leaveBalance.getLeaveType().getName());
        leaveTypeDto.setDescription(leaveBalance.getLeaveType().getDescription());
        leaveTypeDto.setMaxDays(leaveBalance.getLeaveType().getMaxDays());
        leaveTypeDto.setIsPaid(leaveBalance.getLeaveType().getPaid());
        leaveTypeDto.setRequiresApproval(leaveBalance.getLeaveType().getRequiresApproval());
        dto.setLeaveType(leaveTypeDto);

        dto.setBalance(leaveBalance.getBalance());
        dto.setValidUntil(leaveBalance.getValidUntil());
        return dto;
    }
}