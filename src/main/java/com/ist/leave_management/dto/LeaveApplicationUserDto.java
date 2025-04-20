package com.ist.leave_management.dto;

import lombok.Data;

@Data
public class LeaveApplicationUserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String profilePicture;
    private Long departmentId;
    private String departmentName;
}