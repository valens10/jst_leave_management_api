package com.ist.leave_management.service;

import com.ist.leave_management.model.LeaveApplication;
import com.ist.leave_management.model.LeaveWorkflow;
import com.ist.leave_management.model.LeaveWorkflowStatus;
import com.ist.leave_management.model.LeaveStatus;
import com.ist.leave_management.model.LeaveBalance;
import com.ist.user_management.model.User;
import com.ist.leave_management.repository.LeaveWorkflowRepository;
import com.ist.leave_management.repository.LeaveApplicationRepository;
import com.ist.user_management.repository.UserRepository;
import com.ist.common.enums.ERole;
import com.ist.leave_management.repository.LeaveBalanceRepository;
import com.ist.leave_management.model.Holiday;
import com.ist.leave_management.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveWorkflowService {
    @Autowired
    private LeaveWorkflowRepository leaveWorkflowRepository;

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Transactional
    public LeaveWorkflow createWorkflow(LeaveApplication leaveApplication) {
        LeaveWorkflow workflow = new LeaveWorkflow();
        workflow.setLeaveApplication(leaveApplication);
        workflow.setStatus(LeaveWorkflowStatus.PENDING);
        workflow.setComments("Leave application submitted");

        workflow = leaveWorkflowRepository.save(workflow);
        sendNotificationToAdmins(leaveApplication);
        return workflow;
    }

    @Transactional
    public LeaveWorkflow approveLeave(LeaveWorkflow workflow, User approver, String comments) {
        // Check if the leave end date is in the past
        LocalDate today = LocalDate.now();
        LocalDate endDate = workflow.getLeaveApplication().getEndDate();

        if (endDate.isBefore(today)) {
            throw new RuntimeException("Cannot approve leave with end date in the past: " + endDate);
        }

        // Clean up comments
        String cleanComments = cleanComments(comments);

        workflow.setApprovedBy(approver);
        workflow.setApprovedDate(java.time.LocalDateTime.now());
        workflow.setStatus(LeaveWorkflowStatus.APPROVED);
        workflow.setComments(cleanComments);

        // Update leave application status
        LeaveApplication application = workflow.getLeaveApplication();
        application.setStatus(LeaveStatus.APPROVED);
        leaveApplicationRepository.save(application);

        // If this is an annual leave, update the leave balance
        if (application.getLeaveType().getIsAnnualLeave()) {
            updateAnnualLeaveBalance(application);
        }

        return leaveWorkflowRepository.save(workflow);
    }

    @Transactional
    public LeaveWorkflow rejectLeave(LeaveWorkflow workflow, User rejector, String comments) {
        // Clean up comments
        String cleanComments = cleanComments(comments);

        workflow.setRejectedBy(rejector);
        workflow.setRejectedDate(java.time.LocalDateTime.now());
        workflow.setStatus(LeaveWorkflowStatus.REJECTED);
        workflow.setComments(cleanComments);

        // Update leave application status
        LeaveApplication application = workflow.getLeaveApplication();
        application.setStatus(LeaveStatus.REJECTED);
        leaveApplicationRepository.save(application);

        return leaveWorkflowRepository.save(workflow);
    }

    private String cleanComments(String comments) {
        if (comments == null) {
            return null;
        }

        // Remove JSON formatting if present
        if (comments.startsWith("{\"comments\":\"")) {
            comments = comments.substring(12, comments.length() - 2);
        }

        // Remove any extra whitespace and newlines
        return comments.trim();
    }

    private void sendNotificationToAdmins(LeaveApplication leaveApplication) {
        List<User> admins = userRepository.findByRole(ERole.ROLE_ADMIN);
        for (User admin : admins) {
            notificationService.sendNewLeaveApplicationNotification(
                    admin,
                    leaveApplication,
                    leaveApplication.getUser());
        }
    }

    private void updateAnnualLeaveBalance(LeaveApplication application) {
        // Get the user's leave balance for annual leave
        LeaveBalance leaveBalance = leaveBalanceRepository.findByUserIdAndLeaveTypeId(
                application.getUser().getId(),
                application.getLeaveType().getId())
                .orElseThrow(() -> new RuntimeException("Leave balance not found for user"));

        // Calculate days to subtract
        int daysToSubtract;
        if (application.getIsHalfDay()) {
            daysToSubtract = 1; // Half day counts as 1 day
        } else {
            // Get all holidays between start and end date
            List<Holiday> holidays = holidayRepository.findByDateBetween(
                    application.getStartDate(),
                    application.getEndDate());

            // Calculate working days (excluding weekends and holidays)
            daysToSubtract = 0;
            LocalDate currentDate = application.getStartDate();
            while (!currentDate.isAfter(application.getEndDate())) {
                final LocalDate dateToCheck = currentDate; // Create effectively final variable
                // Skip weekends (Saturday and Sunday)
                if (dateToCheck.getDayOfWeek().getValue() < 6) {
                    // Skip holidays
                    boolean isHoliday = holidays.stream()
                            .anyMatch(h -> h.getDate().equals(dateToCheck));
                    if (!isHoliday) {
                        daysToSubtract++;
                    }
                }
                currentDate = currentDate.plusDays(1);
            }
        }

        // Check if user has enough balance
        if (leaveBalance.getBalance() < daysToSubtract) {
            throw new RuntimeException("Insufficient leave balance");
        }

        // Update the balance
        leaveBalance.setBalance(leaveBalance.getBalance() - daysToSubtract);
        leaveBalanceRepository.save(leaveBalance);
    }
}