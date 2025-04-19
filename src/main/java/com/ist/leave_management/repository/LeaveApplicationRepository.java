package com.ist.leave_management.repository;

import com.ist.leave_management.model.LeaveApplication;
import com.ist.leave_management.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {
    List<LeaveApplication> findByUserId(Long userId);

    List<LeaveApplication> findByStatus(LeaveStatus status);
}