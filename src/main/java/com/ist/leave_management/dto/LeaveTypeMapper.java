package com.ist.leave_management.dto;

import com.ist.leave_management.model.LeaveType;
import org.springframework.stereotype.Component;

@Component
public class LeaveTypeMapper {
    public LeaveType toEntity(CreateLeaveTypeDto dto) {
        LeaveType leaveType = new LeaveType();
        leaveType.setName(dto.getName());
        leaveType.setDescription(dto.getDescription());
        leaveType.setMaxDays(dto.getMaxDays());
        leaveType.setPaid(dto.getIsPaid());
        leaveType.setRequiresApproval(dto.getRequiresApproval());
        leaveType.setIsAnnualLeave(dto.getIsAnnualLeave());
        leaveType.setCreatedAt(java.time.LocalDateTime.now());
        leaveType.setUpdatedAt(java.time.LocalDateTime.now());
        return leaveType;
    }

    public LeaveType toEntity(UpdateLeaveTypeDto dto, LeaveType leaveType) {
        leaveType.setName(dto.getName());
        leaveType.setDescription(dto.getDescription());
        leaveType.setMaxDays(dto.getMaxDays());
        leaveType.setPaid(dto.getIsPaid());
        leaveType.setRequiresApproval(dto.getRequiresApproval());
        leaveType.setIsAnnualLeave(dto.getIsAnnualLeave());
        return leaveType;
    }

    public LeaveTypeResponseDto toDto(LeaveType leaveType) {
        LeaveTypeResponseDto dto = new LeaveTypeResponseDto();
        dto.setId(leaveType.getId());
        dto.setName(leaveType.getName());
        dto.setDescription(leaveType.getDescription());
        dto.setMaxDays(leaveType.getMaxDays());
        dto.setIsPaid(leaveType.getPaid());
        dto.setRequiresApproval(leaveType.getRequiresApproval());
        dto.setIsAnnualLeave(leaveType.getIsAnnualLeave());
        dto.setCreatedAt(leaveType.getCreatedAt());
        dto.setUpdatedAt(leaveType.getUpdatedAt());
        return dto;
    }
}