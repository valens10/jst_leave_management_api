package com.ist.leave_management.controller;

import com.ist.common.security.UserDetailsImpl;
import com.ist.common.enums.ERole;
import com.ist.leave_management.dto.*;
import com.ist.leave_management.model.LeaveType;
import com.ist.leave_management.repository.LeaveTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/leaves/types/leave-types")
public class LeaveTypeController {
    private static final Logger logger = LoggerFactory.getLogger(LeaveTypeController.class);

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveTypeMapper leaveTypeMapper;

    @GetMapping
    public ResponseEntity<?> getAllLeaveTypes(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            List<LeaveType> leaveTypes = leaveTypeRepository.findAll();
            List<LeaveTypeResponseDto> response = leaveTypes.stream()
                    .map(leaveTypeMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting all leave types", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching leave types"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLeaveTypeById(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            // Check if the current user is an admin or manager
            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            boolean isAdminOrManager = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(ERole.ROLE_ADMIN.name()) ||
                            auth.getAuthority().equals(ERole.ROLE_MANAGER.name()));

            if (!isAdminOrManager) {
                return ResponseEntity.status(403)
                        .body(new MessageResponseDto("Only admins and managers can view leave types"));
            }

            LeaveType leaveType = leaveTypeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Leave type not found"));

            return ResponseEntity.ok(leaveTypeMapper.toDto(leaveType));
        } catch (RuntimeException e) {
            logger.error("Error getting leave type: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error getting leave type", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching the leave type"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createLeaveType(@RequestBody CreateLeaveTypeDto request, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            // Check if the current user is an admin
            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(ERole.ROLE_ADMIN.name()));

            if (!isAdmin) {
                return ResponseEntity.status(403).body(new MessageResponseDto("Only admins can create leave types"));
            }

            // Check if leave type with same name exists
            if (leaveTypeRepository.existsByName(request.getName())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDto("Leave type with this name already exists"));
            }

            LeaveType leaveType = leaveTypeMapper.toEntity(request);
            leaveType = leaveTypeRepository.save(leaveType);

            return ResponseEntity.ok(leaveTypeMapper.toDto(leaveType));
        } catch (Exception e) {
            logger.error("Error creating leave type", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while creating the leave type"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLeaveType(@PathVariable Long id, @RequestBody UpdateLeaveTypeDto request,
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
                return ResponseEntity.status(403).body(new MessageResponseDto("Only admins can update leave types"));
            }

            LeaveType leaveType = leaveTypeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Leave type not found"));

            // Check if another leave type with the same name exists
            if (!leaveType.getName().equals(request.getName()) &&
                    leaveTypeRepository.existsByName(request.getName())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDto("Leave type with this name already exists"));
            }

            leaveType = leaveTypeMapper.toEntity(request, leaveType);
            leaveType = leaveTypeRepository.save(leaveType);

            return ResponseEntity.ok(leaveTypeMapper.toDto(leaveType));
        } catch (RuntimeException e) {
            logger.error("Error updating leave type: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating leave type", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while updating the leave type"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLeaveType(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            // Check if the current user is an admin
            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(ERole.ROLE_ADMIN.name()));

            if (!isAdmin) {
                return ResponseEntity.status(403).body(new MessageResponseDto("Only admins can delete leave types"));
            }

            if (!leaveTypeRepository.existsById(id)) {
                return ResponseEntity.badRequest().body(new MessageResponseDto("Leave type not found"));
            }

            leaveTypeRepository.deleteById(id);
            return ResponseEntity.ok(new MessageResponseDto("Leave type deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting leave type", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while deleting the leave type"));
        }
    }
}