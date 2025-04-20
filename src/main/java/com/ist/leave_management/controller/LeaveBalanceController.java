package com.ist.leave_management.controller;

import com.ist.common.security.UserDetailsImpl;
import com.ist.common.enums.ERole;
import com.ist.leave_management.dto.*;
import com.ist.leave_management.model.LeaveBalance;
import com.ist.leave_management.repository.LeaveBalanceRepository;
import com.ist.leave_management.repository.LeaveTypeRepository;
import com.ist.user_management.repository.UserRepository;
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
@RequestMapping("/api/leave-balances")
public class LeaveBalanceController {
    private static final Logger logger = LoggerFactory.getLogger(LeaveBalanceController.class);

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceMapper leaveBalanceMapper;

    @GetMapping
    public ResponseEntity<?> getAllLeaveBalances(Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can view all leave balances"));
            }

            List<LeaveBalance> balances = leaveBalanceRepository.findAll();
            List<LeaveBalanceDto> response = balances.stream()
                    .map(leaveBalanceMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting all leave balances", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching leave balances"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createLeaveBalance(@RequestBody CreateLeaveBalanceDto request,
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
                return ResponseEntity.status(403).body(new MessageResponseDto("Only admins can create leave balances"));
            }

            var user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            var leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                    .orElseThrow(() -> new RuntimeException("Leave type not found"));

            // Check if balance already exists
            if (leaveBalanceRepository.findByUserIdAndLeaveTypeId(request.getUserId(), request.getLeaveTypeId())
                    .isPresent()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDto("Leave balance already exists for this user and leave type"));
            }

            LeaveBalance leaveBalance = leaveBalanceMapper.toEntity(request);
            leaveBalance.setUser(user);
            leaveBalance.setLeaveType(leaveType);
            leaveBalance = leaveBalanceRepository.save(leaveBalance);

            return ResponseEntity.ok(leaveBalanceMapper.toDto(leaveBalance));
        } catch (RuntimeException e) {
            logger.error("Error creating leave balance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating leave balance", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while creating the leave balance"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLeaveBalance(@PathVariable Long id, @RequestBody UpdateLeaveBalanceDto request,
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
                return ResponseEntity.status(403).body(new MessageResponseDto("Only admins can update leave balances"));
            }

            LeaveBalance leaveBalance = leaveBalanceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Leave balance not found"));

            leaveBalance = leaveBalanceMapper.toEntity(request, leaveBalance);
            leaveBalance = leaveBalanceRepository.save(leaveBalance);

            return ResponseEntity.ok(leaveBalanceMapper.toDto(leaveBalance));
        } catch (RuntimeException e) {
            logger.error("Error updating leave balance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating leave balance", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while updating the leave balance"));
        }
    }

    @GetMapping("/my-balances")
    public ResponseEntity<?> getMyLeaveBalances(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            List<LeaveBalance> balances = leaveBalanceRepository.findByUserId(currentUser.getId());
            List<LeaveBalanceDto> response = balances.stream()
                    .map(leaveBalanceMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting user's leave balances", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching leave balances"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLeaveBalance(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            // Check if the current user is an admin
            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(ERole.ROLE_ADMIN.name()));

            if (!isAdmin) {
                return ResponseEntity.status(403)
                        .body(new MessageResponseDto("Only admins can delete leave balances"));
            }

            LeaveBalance balance = leaveBalanceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Leave balance not found"));

            leaveBalanceRepository.delete(balance);
            return ResponseEntity.ok(new MessageResponseDto("Leave balance deleted successfully"));
        } catch (RuntimeException e) {
            logger.error("Error deleting leave balance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting leave balance", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while deleting the leave balance"));
        }
    }
}