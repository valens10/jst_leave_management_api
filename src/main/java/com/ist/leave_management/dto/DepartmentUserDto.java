package com.ist.leave_management.dto;

import lombok.Data;

@Data
public class DepartmentUserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String profilePicture;
    private Long departmentId;
}