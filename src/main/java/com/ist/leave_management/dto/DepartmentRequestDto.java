package com.ist.leave_management.dto;

import lombok.Data;
import java.util.List;

@Data
public class DepartmentRequestDto {
    private String name;
    private String description;
    private List<Long> userIds;
}