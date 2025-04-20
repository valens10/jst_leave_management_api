package com.ist.leave_management.dto;

import com.ist.user_management.model.User;
import com.ist.leave_management.model.LeaveApplication;
import com.ist.leave_management.model.LeaveType;
import com.ist.leave_management.model.LeaveStatus;
import com.ist.leave_management.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.LocalDate;

@Component
public class LeaveApplicationMapper {
    @Autowired
    private FileStorageService fileStorageService;

    public LeaveApplication toEntity(CreateLeaveApplicationDto dto, User user, LeaveType leaveType) {
        // Validate dates
        LocalDate today = LocalDate.now();
        if (dto.getStartDate().isBefore(today)) {
            throw new RuntimeException("Start date cannot be in the past");
        }

        // If half day, end date is same as start date
        LocalDate endDate;
        if (dto.getIsHalfDay() != null && dto.getIsHalfDay()) {
            endDate = dto.getStartDate();
        } else {
            endDate = dto.getEndDate() != null ? dto.getEndDate() : dto.getStartDate();
        }

        if (endDate.isBefore(today)) {
            throw new RuntimeException("End date cannot be in the past");
        }

        if (endDate.isBefore(dto.getStartDate())) {
            throw new RuntimeException("End date cannot be before start date");
        }

        LeaveApplication leaveApplication = new LeaveApplication();
        leaveApplication.setUser(user);
        leaveApplication.setLeaveType(leaveType);
        leaveApplication.setStartDate(dto.getStartDate());
        leaveApplication.setEndDate(endDate);
        leaveApplication.setIsHalfDay(dto.getIsHalfDay() != null ? dto.getIsHalfDay() : false);

        // Handle file upload
        if (dto.getAttachment() != null && !dto.getAttachment().isEmpty()) {
            try {
                String storedFileName = fileStorageService.storeFile(dto.getAttachment());
                leaveApplication.setAttachment(storedFileName);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store file: " + e.getMessage());
            }
        }

        leaveApplication.setStatus(LeaveStatus.PENDING);
        leaveApplication.setReason(dto.getReason());
        return leaveApplication;
    }

    public LeaveApplicationResponseDto toDto(LeaveApplication leaveApplication) {
        LeaveApplicationResponseDto dto = new LeaveApplicationResponseDto();
        dto.setId(leaveApplication.getId());

        // Map user to DTO
        LeaveApplicationUserDto userDto = new LeaveApplicationUserDto();
        User user = leaveApplication.getUser();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setProfilePicture(user.getProfilePicture());
        if (user.getDepartment() != null) {
            userDto.setDepartmentId(user.getDepartment().getId());
            userDto.setDepartmentName(user.getDepartment().getName());
        }
        dto.setUser(userDto);

        dto.setLeaveTypeId(leaveApplication.getLeaveType().getId());
        dto.setLeaveTypeName(leaveApplication.getLeaveType().getName());
        dto.setLeaveType(leaveApplication.getLeaveType());
        dto.setStartDate(leaveApplication.getStartDate());
        dto.setEndDate(leaveApplication.getEndDate());
        dto.setIsHalfDay(leaveApplication.getIsHalfDay());
        dto.setAttachment(leaveApplication.getAttachment());
        dto.setStatus(leaveApplication.getStatus());
        dto.setReason(leaveApplication.getReason());
        dto.setCreatedAt(leaveApplication.getCreatedAt());
        dto.setUpdatedAt(leaveApplication.getUpdatedAt());
        return dto;
    }
}