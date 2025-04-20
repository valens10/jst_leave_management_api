package com.ist.leave_management.dto;

import com.ist.leave_management.model.Department;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class DepartmentResponseDto {
    private Long id;
    private String name;
    private String description;
    private List<DepartmentUserDto> users;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DepartmentResponseDto fromEntity(Department department) {
        DepartmentResponseDto dto = new DepartmentResponseDto();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());
        dto.setCreatedAt(department.getCreatedAt());
        dto.setUpdatedAt(department.getUpdatedAt());

        // Only map users if they exist
        if (department.getUsers() != null) {
            dto.setUsers(department.getUsers().stream()
                    .map(user -> {
                        DepartmentUserDto userDto = new DepartmentUserDto();
                        userDto.setId(user.getId());
                        userDto.setEmail(user.getEmail());
                        userDto.setFirstName(user.getFirstName());
                        userDto.setLastName(user.getLastName());
                        userDto.setDepartmentId(user.getDepartment() != null ? user.getDepartment().getId() : null);
                        return userDto;
                    })
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}