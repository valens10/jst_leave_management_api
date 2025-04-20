package com.ist.leave_management.dto;

import com.ist.leave_management.model.Holiday;
import org.springframework.stereotype.Component;

@Component
public class HolidayMapper {
    public Holiday toEntity(CreateHolidayDto dto) {
        Holiday holiday = new Holiday();
        holiday.setName(dto.getName());
        holiday.setDate(dto.getDate());
        holiday.setDescription(dto.getDescription());
        return holiday;
    }

    public Holiday toEntity(UpdateHolidayDto dto, Holiday holiday) {
        holiday.setName(dto.getName());
        holiday.setDate(dto.getDate());
        holiday.setDescription(dto.getDescription());
        return holiday;
    }

    public HolidayDto toDto(Holiday holiday) {
        HolidayDto dto = new HolidayDto();
        dto.setId(holiday.getId());
        dto.setName(holiday.getName());
        dto.setDate(holiday.getDate());
        dto.setDescription(holiday.getDescription());
        dto.setCreatedAt(holiday.getCreatedAt());
        dto.setUpdatedAt(holiday.getUpdatedAt());
        return dto;
    }
}