package com.ist.leave_management.controller;

import com.ist.common.security.UserDetailsImpl;
import com.ist.common.enums.ERole;
import com.ist.leave_management.dto.*;
import com.ist.leave_management.model.LeaveApplication;
import com.ist.leave_management.model.LeaveStatus;
import com.ist.leave_management.model.LeaveWorkflow;
import com.ist.leave_management.model.LeaveWorkflowStatus;
import com.ist.leave_management.repository.LeaveApplicationRepository;
import com.ist.leave_management.repository.LeaveTypeRepository;
import com.ist.leave_management.repository.LeaveWorkflowRepository;
import com.ist.leave_management.service.FileStorageService;
import com.ist.leave_management.service.LeaveWorkflowService;
import com.ist.user_management.repository.UserRepository;
import com.ist.leave_management.service.NotificationService;
import com.ist.user_management.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/leaves")
public class LeaveController {
    private static final Logger logger = LoggerFactory.getLogger(LeaveController.class);

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveApplicationMapper leaveApplicationMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private LeaveWorkflowService leaveWorkflowService;

    @Autowired
    private LeaveWorkflowRepository leaveWorkflowRepository;

    @Autowired
    private LeaveWorkflowMapper leaveWorkflowMapper;

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getAllLeaves(Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can view all leaves"));
            }

            // Get all leave applications
            List<LeaveApplication> applications = leaveApplicationRepository.findAll();
            List<LeaveApplicationResponseDto> response = applications.stream()
                    .map(leaveApplicationMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting all leaves", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching leaves"));
        }
    }

    @PostMapping(value = "/request", consumes = "multipart/form-data")
    public ResponseEntity<?> requestLeave(
            @RequestParam("leaveTypeId") Long leaveTypeId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "isHalfDay", required = false) Boolean isHalfDay,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment,
            @RequestParam(value = "reason", required = false) String reason,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            var user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Debug logging
            logger.debug("Attempting to find leave type with ID: {}", leaveTypeId);
            var leaveType = leaveTypeRepository.findById(leaveTypeId)
                    .orElseThrow(() -> new RuntimeException("Leave type not found with ID: " + leaveTypeId));

            logger.debug("Found leave type: {}", leaveType);
            if (leaveType == null) {
                logger.error("Leave type is null for ID: {}", leaveTypeId);
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDto("Invalid leave type ID: " + leaveTypeId));
            }

            // Create DTO from form data
            CreateLeaveApplicationDto request = new CreateLeaveApplicationDto();
            request.setLeaveTypeId(leaveTypeId);
            request.setStartDate(startDate);
            request.setEndDate(endDate);
            request.setIsHalfDay(isHalfDay);
            request.setAttachment(attachment);
            request.setReason(reason);

            logger.debug("Creating leave application with leave type ID: {}", leaveTypeId);
            LeaveApplication leaveApplication = leaveApplicationMapper.toEntity(request, user, leaveType);
            leaveApplication = leaveApplicationRepository.save(leaveApplication);
            logger.debug("Successfully created leave application with ID: {}", leaveApplication.getId());

            // Create workflow for the leave application
            LeaveWorkflow workflow = leaveWorkflowService.createWorkflow(leaveApplication);
            logger.debug("Created workflow for leave application: {}", workflow.getId());

            // Send notification to admins and managers about new leave application
            List<User> admins = userRepository.findByRole(ERole.ROLE_ADMIN);
            List<User> managers = userRepository.findByRole(ERole.ROLE_MANAGER);

            // Combine lists and send notifications
            List<User> adminsAndManagers = new ArrayList<>(admins);
            adminsAndManagers.addAll(managers);

            for (User adminOrManager : adminsAndManagers) {
                notificationService.sendNewLeaveApplicationNotification(
                        adminOrManager,
                        leaveApplication,
                        user);
            }

            return ResponseEntity.ok(leaveApplicationMapper.toDto(leaveApplication));
        } catch (RuntimeException e) {
            logger.error("Error creating leave application: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating leave application", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while creating the leave application"));
        }
    }

    @GetMapping("/my-leaves")
    public ResponseEntity<?> getMyLeaves(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            List<LeaveApplication> applications = leaveApplicationRepository.findByUserId(currentUser.getId());
            List<LeaveApplicationResponseDto> response = applications.stream()
                    .map(leaveApplicationMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting user's leave applications", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching leave applications"));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingLeaves(Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can view pending leaves"));
            }

            List<LeaveApplication> applications = leaveApplicationRepository.findByStatus(LeaveStatus.PENDING);
            List<LeaveApplicationResponseDto> response = applications.stream()
                    .map(leaveApplicationMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting pending leave applications", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching pending applications"));
        }
    }

    @GetMapping("/pending/count")
    public ResponseEntity<?> getPendingLeavesCount(Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can view pending leaves count"));
            }

            long count = leaveApplicationRepository.findByStatus(LeaveStatus.PENDING).size();
            return ResponseEntity.ok(new CountResponseDto(count));
        } catch (Exception e) {
            logger.error("Error getting pending leaves count", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching pending leaves count"));
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveLeave(@PathVariable Long id, @RequestBody WorkflowActionDto actionDto,
            Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can approve leaves"));
            }

            var approver = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Approver not found"));

            LeaveApplication leaveApplication = leaveApplicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Leave application not found"));

            LeaveWorkflow workflow = leaveWorkflowRepository.findByLeaveApplication(leaveApplication)
                    .orElseThrow(() -> new RuntimeException("Workflow not found"));

            if (workflow.getStatus() != LeaveWorkflowStatus.PENDING) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDto("Only pending leaves can be approved"));
            }

            workflow = leaveWorkflowService.approveLeave(workflow, approver, actionDto.getComments());

            // Send notification to the applicant
            notificationService.sendLeaveApprovalNotification(
                    leaveApplication.getUser(),
                    leaveApplication,
                    approver);

            return ResponseEntity.ok(leaveWorkflowMapper.toDto(workflow));
        } catch (RuntimeException e) {
            logger.error("Error approving leave: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error approving leave", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while approving the leave"));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectLeave(@PathVariable Long id, @RequestBody WorkflowActionDto actionDto,
            Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can reject leaves"));
            }

            var rejector = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Rejector not found"));

            LeaveApplication leaveApplication = leaveApplicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Leave application not found"));

            LeaveWorkflow workflow = leaveWorkflowRepository.findByLeaveApplication(leaveApplication)
                    .orElseThrow(() -> new RuntimeException("Workflow not found"));

            if (workflow.getStatus() != LeaveWorkflowStatus.PENDING) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDto("Only pending leaves can be rejected"));
            }

            workflow = leaveWorkflowService.rejectLeave(workflow, rejector, actionDto.getComments());

            // Send notification to the applicant
            notificationService.sendLeaveRejectionNotification(
                    leaveApplication.getUser(),
                    leaveApplication,
                    rejector,
                    actionDto.getComments());

            return ResponseEntity.ok(leaveWorkflowMapper.toDto(workflow));
        } catch (RuntimeException e) {
            logger.error("Error rejecting leave: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error rejecting leave", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while rejecting the leave"));
        }
    }

    @GetMapping("/attachments/{fileName}")
    public ResponseEntity<?> downloadAttachment(@PathVariable String fileName) {
        try {
            // Load file as Resource
            Path filePath = fileStorageService.loadFile(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = determineContentType(fileName);

            // Set headers for inline viewing
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("inline", fileName);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading attachment: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("Error downloading attachment: " + e.getMessage()));
        }
    }

    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> "application/octet-stream";
        };
    }

    @GetMapping("/current_leaves")
    public ResponseEntity<?> getCurrentLeaves(Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can view current leaves"));
            }

            // Get current date
            final LocalDate today = LocalDate.now();
            logger.debug("Fetching current leaves for date: {}", today);

            // First, get all leaves to check what's in the database
            List<LeaveApplication> allLeaves = leaveApplicationRepository.findAll();
            // Log all leaves for debugging
            allLeaves.forEach(leave -> logger.debug("Leave: id={}, start={}, end={}, status={}, halfDay={}",
                    leave.getId(), leave.getStartDate(), leave.getEndDate(),
                    leave.getStatus(), leave.getIsHalfDay()));

            // Get all approved leave applications for today
            List<LeaveApplication> currentLeaves = allLeaves.stream()
                    .filter(application -> {
                        logger.debug("Checking application: id={}, start={}, end={}, status={}, halfDay={}",
                                application.getId(), application.getStartDate(),
                                application.getEndDate(), application.getStatus(),
                                application.getIsHalfDay());

                        // Check if the leave is approved
                        if (application.getStatus() != LeaveStatus.APPROVED) {
                            logger.debug("Application {} not approved, status: {}",
                                    application.getId(), application.getStatus());
                            return false;
                        }

                        // For half-day leaves, check if the date matches today
                        if (application.getIsHalfDay()) {
                            boolean matches = application.getStartDate().equals(today);
                            logger.debug("Half-day leave {} matches today: {}",
                                    application.getId(), matches);
                            return matches;
                        }

                        // For full-day leaves, check if today is within the leave period
                        boolean isWithinPeriod = !today.isBefore(application.getStartDate()) &&
                                !today.isAfter(application.getEndDate());
                        logger.debug("Full-day leave {} is within period: {}",
                                application.getId(), isWithinPeriod);
                        return isWithinPeriod;
                    })
                    .collect(Collectors.toList());

            logger.debug("Found {} current leaves", currentLeaves.size());

            // Map to DTOs
            List<LeaveApplicationResponseDto> response = currentLeaves.stream()
                    .map(leaveApplicationMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting current leaves", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching current leaves"));
        }
    }

    @GetMapping("/workflows/pending")
    public ResponseEntity<?> getPendingWorkflows(Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can view pending workflows"));
            }

            List<LeaveWorkflow> workflows = leaveWorkflowRepository.findByStatus(LeaveWorkflowStatus.PENDING);
            List<LeaveWorkflowDto> response = workflows.stream()
                    .map(leaveWorkflowMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting pending workflows", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching pending workflows"));
        }
    }

    @GetMapping("/workflows/my-workflows")
    public ResponseEntity<?> getMyWorkflows(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            var user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get all leave applications for the user
            List<LeaveApplication> applications = leaveApplicationRepository.findByUser(user);

            // Get workflows for each application
            List<LeaveWorkflow> workflows = new ArrayList<>();
            for (LeaveApplication application : applications) {
                leaveWorkflowRepository.findByLeaveApplication(application)
                        .ifPresent(workflows::add);
            }

            List<LeaveWorkflowDto> response = workflows.stream()
                    .map(leaveWorkflowMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error getting user workflows: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error getting user workflows", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching workflows"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLeave(@PathVariable Long id, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            var user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            LeaveApplication leaveApplication = leaveApplicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Leave application not found"));

            // Check if the leave belongs to the current user
            if (!leaveApplication.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403)
                        .body(new MessageResponseDto("You can only delete your own leave applications"));
            }

            // Check if the leave is still pending
            if (leaveApplication.getStatus() != LeaveStatus.PENDING) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDto("Only pending leaves can be deleted"));
            }

            // Delete the workflow if it exists
            leaveWorkflowRepository.findByLeaveApplication(leaveApplication)
                    .ifPresent(workflow -> leaveWorkflowRepository.delete(workflow));

            // Delete the leave application
            leaveApplicationRepository.delete(leaveApplication);

            return ResponseEntity.ok(new MessageResponseDto("Leave application deleted successfully"));
        } catch (RuntimeException e) {
            logger.error("Error deleting leave: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting leave", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while deleting the leave application"));
        }
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getLeavesByDepartment(@PathVariable Long departmentId, Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can view department leaves"));
            }

            // Get all leave applications for users in the department
            List<LeaveApplication> applications = leaveApplicationRepository.findByUser_Department_Id(departmentId);

            // Map to DTOs
            List<LeaveApplicationResponseDto> response = applications.stream()
                    .map(leaveApplicationMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting department leaves", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while fetching department leaves"));
        }
    }

    @GetMapping("/reports/employee/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getEmployeeReport(
            @PathVariable Long userId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(new MessageResponseDto("Authentication required"));
            }

            // Check if the current user is an admin, manager, or the employee themselves
            UserDetailsImpl currentUser = (UserDetailsImpl) authentication.getPrincipal();
            boolean isAdminOrManager = currentUser.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(ERole.ROLE_ADMIN.name()) ||
                            auth.getAuthority().equals(ERole.ROLE_MANAGER.name()));

            if (!isAdminOrManager && !currentUser.getId().equals(userId)) {
                return ResponseEntity.status(403)
                        .body(new MessageResponseDto("You can only view your own reports"));
            }

            List<LeaveApplication> applications;
            if (startDate != null && endDate != null) {
                applications = leaveApplicationRepository.findByUserIdAndStartDateBetween(userId, startDate, endDate);
            } else {
                applications = leaveApplicationRepository.findByUserId(userId);
            }

            List<LeaveApplicationResponseDto> response = applications.stream()
                    .map(leaveApplicationMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating employee report", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while generating the report"));
        }
    }

    @GetMapping("/reports/leave-type/{leaveTypeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getLeaveTypeReport(
            @PathVariable Long leaveTypeId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can view leave type reports"));
            }

            List<LeaveApplication> applications;
            if (startDate != null && endDate != null) {
                applications = leaveApplicationRepository.findByLeaveTypeIdAndStartDateBetween(leaveTypeId, startDate,
                        endDate);
            } else {
                applications = leaveApplicationRepository.findByLeaveTypeId(leaveTypeId);
            }

            List<LeaveApplicationResponseDto> response = applications.stream()
                    .map(leaveApplicationMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating leave type report", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while generating the report"));
        }
    }

    @GetMapping("/reports/department/{departmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getDepartmentReport(
            @PathVariable Long departmentId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can view department reports"));
            }

            List<LeaveApplication> applications;
            if (startDate != null && endDate != null) {
                applications = leaveApplicationRepository.findByUser_Department_IdAndStartDateBetween(departmentId,
                        startDate, endDate);
            } else {
                applications = leaveApplicationRepository.findByUser_Department_Id(departmentId);
            }

            List<LeaveApplicationResponseDto> response = applications.stream()
                    .map(leaveApplicationMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating department report", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while generating the report"));
        }
    }

    @GetMapping("/reports/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSummaryReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication) {
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
                        .body(new MessageResponseDto("Only admins and managers can view summary reports"));
            }

            List<LeaveApplication> applications;
            if (startDate != null && endDate != null) {
                applications = leaveApplicationRepository.findByStartDateBetween(startDate, endDate);
            } else {
                applications = leaveApplicationRepository.findAll();
            }

            // Group by status
            Map<LeaveStatus, Long> statusCount = applications.stream()
                    .collect(Collectors.groupingBy(LeaveApplication::getStatus, Collectors.counting()));

            // Group by leave type
            Map<String, Long> leaveTypeCount = applications.stream()
                    .collect(Collectors.groupingBy(
                            app -> app.getLeaveType().getName(),
                            Collectors.counting()));

            // Group by department
            Map<String, Long> departmentCount = applications.stream()
                    .collect(Collectors.groupingBy(
                            app -> app.getUser().getDepartment().getName(),
                            Collectors.counting()));

            // Create summary DTO
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalApplications", applications.size());
            summary.put("statusCount", statusCount);
            summary.put("leaveTypeCount", leaveTypeCount);
            summary.put("departmentCount", departmentCount);

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error generating summary report", e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponseDto("An error occurred while generating the report"));
        }
    }
}