package com.ist.leave_management.dto;

import lombok.Data;

@Data
public class CountResponseDto {
    private long count;

    public CountResponseDto(long count) {
        this.count = count;
    }
}