package com.ist.user_management.controller;

import com.ist.common.security.UserDetailsImpl;
import com.ist.common.enums.ERole;
import com.ist.user_management.model.Role;
import com.ist.user_management.model.User;
import com.ist.user_management.dto.JwtResponseDto;
import com.ist.user_management.dto.MessageResponseDto;
import com.ist.user_management.dto.AssignRoleRequestDto;
import com.ist.user_management.repository.RoleRepository;
import com.ist.user_management.repository.UserRepository;
import com.ist.leave_management.repository.LeaveApplicationRepository;
import com.ist.leave_management.repository.LeaveBalanceRepository;
import com.ist.leave_management.repository.LeaveWorkflowRepository;
import com.ist.leave_management.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UsersController {
    private static final Logger logger = LoggerFactory.getLogger(UsersController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveWorkflowRepository leaveWorkflowRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/user_details")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.badRequest().body(new MessageResponseDto("No authenticated user found"));
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new JwtResponseDto(
                    null,
                    userDetails.getId(),
                    userDetails.getEmail(),
                    userDetails.getFirstName(),
                    userDetails.getLastName(),
                    userDetails.getProfilePicture(),
                    roles));
        } catch (Exception e) {
            logger.error("Error getting current user details", e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDto("Error getting user details: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can view all users"));
            }

            // Get all users and map them to DTOs
            List<JwtResponseDto> users = userRepository.findAll().stream()
                    .map(user -> {
                        List<String> roles = user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .collect(Collectors.toList());

                        return new JwtResponseDto(
                                null,
                                user.getId(),
                                user.getEmail(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getProfilePicture(),
                                roles);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error getting all users", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching users"));
        }
    }

    @PostMapping(value = "/assign_role", consumes = "application/json")
    public ResponseEntity<?> assignRole(@RequestBody AssignRoleRequestDto request, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            // Check if the current user is an admin
            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(ERole.ROLE_ADMIN.name()));

            if (!isAdmin) {
                return ResponseEntity.status(403).body(new MessageResponseDto("Only admins can assign roles"));
            }

            // Find the user to update
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Find the role to assign
            Role role = roleRepository.findByName(request.getRole())
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            // Clear existing roles and set the new role
            user.getRoles().clear();
            user.getRoles().add(role);
            userRepository.save(user);

            return ResponseEntity.ok(new MessageResponseDto("Role assigned successfully"));
        } catch (RuntimeException e) {
            logger.error("Error assigning role: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error assigning role", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An unexpected error occurred"));
        }
    }

    @DeleteMapping("/{userId}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long userId, Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can delete users"));
            }

            // Check if user exists
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            try {
                // 1. Delete workflows where user is approver/rejector
                leaveWorkflowRepository.deleteByApprovedByOrRejectedBy(user, user);

                // 2. Delete notifications
                notificationRepository.deleteByUser(user);

                // 3. Delete leave applications
                leaveApplicationRepository.deleteByUser(user);

                // 4. Delete leave balances
                leaveBalanceRepository.deleteByUser(user);

                // 5. Finally delete the user
                userRepository.delete(user);

                return ResponseEntity.ok(new MessageResponseDto("User deleted successfully"));
            } catch (Exception e) {
                logger.error("Error during user deletion process", e);
                return ResponseEntity.internalServerError()
                        .body(new MessageResponseDto("Error during user deletion: " + e.getMessage()));
            }
        } catch (Exception e) {
            logger.error("Error in deleteUser endpoint", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while processing the request"));
        }
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getAllRoles(Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can view roles"));
            }

            // Get all roles
            List<Role> roles = roleRepository.findAll();
            List<String> roleNames = roles.stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(roleNames);
        } catch (Exception e) {
            logger.error("Error getting all roles", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching roles"));
        }
    }
}