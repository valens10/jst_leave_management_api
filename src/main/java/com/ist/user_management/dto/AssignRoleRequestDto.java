package com.ist.user_management.dto;

import com.ist.common.enums.ERole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignRoleRequestDto {
    @NotNull
    private Long userId;

    @NotNull
    private ERole role;
}