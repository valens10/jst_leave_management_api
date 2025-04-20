package com.ist.leave_management.repository;

import com.ist.leave_management.model.LeaveApplication;
import com.ist.leave_management.model.LeaveStatus;
import com.ist.user_management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {
    List<LeaveApplication> findByUserId(Long userId);

    List<LeaveApplication> findByStatus(LeaveStatus status);

    List<LeaveApplication> findByUser(User user);

    void deleteByUser(User user);

    long countByStatus(LeaveStatus status);

    List<LeaveApplication> findByUser_Department_Id(Long departmentId);

    // New methods for reports
    List<LeaveApplication> findByUserIdAndStartDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    List<LeaveApplication> findByLeaveTypeIdAndStartDateBetween(Long leaveTypeId, LocalDate startDate,
            LocalDate endDate);

    List<LeaveApplication> findByLeaveTypeId(Long leaveTypeId);

    List<LeaveApplication> findByUser_Department_IdAndStartDateBetween(Long departmentId, LocalDate startDate,
            LocalDate endDate);

    List<LeaveApplication> findByStartDateBetween(LocalDate startDate, LocalDate endDate);
}