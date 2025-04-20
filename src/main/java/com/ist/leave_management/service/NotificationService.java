package com.ist.leave_management.service;

import com.ist.leave_management.dto.NotificationDto;
import com.ist.leave_management.model.LeaveApplication;
import com.ist.leave_management.model.Notification;
import com.ist.user_management.model.User;
import com.ist.leave_management.repository.NotificationRepository;
import com.ist.leave_management.dto.NotificationMapper;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    @Autowired
    private SendGrid sendGrid;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationMapper notificationMapper;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    @Transactional
    public Notification createNotification(User user, String title, String message, String type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalse(user)
                .stream()
                .map(notificationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalse(user)
                .stream()
                .map(notificationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to mark this notification as read");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository.findByUserAndIsReadFalse(user);
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    public void sendLeaveApprovalNotification(User recipient, LeaveApplication leaveApplication, User approver) {
        String subject = "Leave Application Approved";
        String content = String.format(
                "Dear %s %s,\n\nYour leave application #%d has been approved by %s %s.\n\nLeave Details:\n- Type: %s\n- Start Date: %s\n- End Date: %s\n\nBest regards,\nLeave Management System",
                recipient.getFirstName(),
                recipient.getLastName(),
                leaveApplication.getId(),
                approver.getFirstName(),
                approver.getLastName(),
                leaveApplication.getLeaveType().getName(),
                leaveApplication.getStartDate(),
                leaveApplication.getEndDate());

        sendEmail(recipient.getEmail(), subject, content);
        createNotification(recipient, subject, content, "leave_approval");
    }

    public void sendLeaveRejectionNotification(User recipient, LeaveApplication leaveApplication, User rejector,
            String comments) {
        String subject = "Leave Application Rejected";
        String content = String.format(
                "Dear %s %s,\n\nYour leave application #%d has been rejected by %s %s.\n\nComments: %s\n\nLeave Details:\n- Type: %s\n- Start Date: %s\n- End Date: %s\n\nBest regards,\nLeave Management System",
                recipient.getFirstName(),
                recipient.getLastName(),
                leaveApplication.getId(),
                rejector.getFirstName(),
                rejector.getLastName(),
                comments,
                leaveApplication.getLeaveType().getName(),
                leaveApplication.getStartDate(),
                leaveApplication.getEndDate());

        sendEmail(recipient.getEmail(), subject, content);
        createNotification(recipient, subject, content, "leave_rejection");
    }

    public void sendNewLeaveApplicationNotification(User recipient, LeaveApplication leaveApplication, User applicant) {
        String subject = "New Leave Application for Approval";
        String content = String.format(
                "Dear %s %s,\n\nA new leave application #%d has been submitted by %s %s.\n\nLeave Details:\n- Type: %s\n- Start Date: %s\n- End Date: %s\n\nPlease review and take appropriate action.\n\nBest regards,\nLeave Management System",
                recipient.getFirstName(),
                recipient.getLastName(),
                leaveApplication.getId(),
                applicant.getFirstName(),
                applicant.getLastName(),
                leaveApplication.getLeaveType().getName(),
                leaveApplication.getStartDate(),
                leaveApplication.getEndDate());

        sendEmail(recipient.getEmail(), subject, content);
        createNotification(recipient, subject, content, "new_leave_application");
    }

    private void sendEmail(String toEmail, String subject, String content) {
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        Content emailContent = new Content("text/plain", content);
        Mail mail = new Mail(from, subject, to, emailContent);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 400) {
                System.err.println("Error sending email. Status code: " + response.getStatusCode());
                System.err.println("Response body: " + response.getBody());
                System.err.println("Response headers: " + response.getHeaders());

                if (response.getStatusCode() == 401) {
                    throw new RuntimeException("SendGrid authentication failed. Please check your API key.");
                }
            } else {
                System.out.println("Email sent successfully with status code: " + response.getStatusCode());
            }
        } catch (IOException ex) {
            System.err.println("Error sending email: " + ex.getMessage());
            throw new RuntimeException("Failed to send email: " + ex.getMessage());
        }
    }
}