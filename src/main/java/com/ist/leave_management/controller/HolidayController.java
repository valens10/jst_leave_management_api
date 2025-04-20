package com.ist.leave_management.controller;

import com.ist.common.security.UserDetailsImpl;
import com.ist.common.enums.ERole;
import com.ist.leave_management.dto.*;
import com.ist.leave_management.model.Holiday;
import com.ist.leave_management.repository.HolidayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/holidays")
public class HolidayController {
    private static final Logger logger = LoggerFactory.getLogger(HolidayController.class);

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private HolidayMapper holidayMapper;

    @GetMapping
    public ResponseEntity<?> getAllHolidays(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            List<Holiday> holidays = holidayRepository.findAll();
            List<HolidayDto> response = holidays.stream()
                    .map(holidayMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting all holidays", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching holidays"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getHolidayById(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            Holiday holiday = holidayRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Holiday not found"));

            return ResponseEntity.ok(holidayMapper.toDto(holiday));
        } catch (RuntimeException e) {
            logger.error("Error getting holiday: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error getting holiday", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching the holiday"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createHoliday(@RequestBody CreateHolidayDto request, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            // Check if the current user is an admin
            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(ERole.ROLE_ADMIN.name()));

            if (!isAdmin) {
                return ResponseEntity.status(403).body(new MessageResponseDto("Only admins can create holidays"));
            }

            // Check if holiday with same name exists
            if (holidayRepository.existsByName(request.getName())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDto("Holiday with this name already exists"));
            }

            Holiday holiday = holidayMapper.toEntity(request);
            holiday = holidayRepository.save(holiday);

            return ResponseEntity.ok(holidayMapper.toDto(holiday));
        } catch (Exception e) {
            logger.error("Error creating holiday", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while creating the holiday"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateHoliday(@PathVariable Long id, @RequestBody UpdateHolidayDto request,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            // Check if the current user is an admin
            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(ERole.ROLE_ADMIN.name()));

            if (!isAdmin) {
                return ResponseEntity.status(403).body(new MessageResponseDto("Only admins can update holidays"));
            }

            Holiday holiday = holidayRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Holiday not found"));

            // Check if another holiday with the same name exists
            if (!holiday.getName().equals(request.getName()) &&
                    holidayRepository.existsByName(request.getName())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDto("Holiday with this name already exists"));
            }

            holiday = holidayMapper.toEntity(request, holiday);
            holiday = holidayRepository.save(holiday);

            return ResponseEntity.ok(holidayMapper.toDto(holiday));
        } catch (RuntimeException e) {
            logger.error("Error updating holiday: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating holiday", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while updating the holiday"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHoliday(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            // Check if the current user is an admin
            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(ERole.ROLE_ADMIN.name()));

            if (!isAdmin) {
                return ResponseEntity.status(403).body(new MessageResponseDto("Only admins can delete holidays"));
            }

            Holiday holiday = holidayRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Holiday not found"));

            holidayRepository.delete(holiday);

            return ResponseEntity.ok(new MessageResponseDto("Holiday deleted successfully"));
        } catch (RuntimeException e) {
            logger.error("Error deleting holiday: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting holiday", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while deleting the holiday"));
        }
    }

    @GetMapping("/between")
    public ResponseEntity<?> getHolidaysBetweenDates(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            List<Holiday> holidays = holidayRepository.findByDateBetween(startDate, endDate);
            List<HolidayDto> response = holidays.stream()
                    .map(holidayMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting holidays between dates", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching holidays"));
        }
    }
}