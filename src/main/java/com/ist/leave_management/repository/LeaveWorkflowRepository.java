package com.ist.leave_management.repository;

import com.ist.leave_management.model.LeaveApplication;
import com.ist.leave_management.model.LeaveWorkflow;
import com.ist.leave_management.model.LeaveWorkflowStatus;
import com.ist.user_management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveWorkflowRepository extends JpaRepository<LeaveWorkflow, Long> {
    Optional<LeaveWorkflow> findByLeaveApplicationId(Long leaveApplicationId);

    List<LeaveWorkflow> findByStatus(LeaveWorkflowStatus status);

    List<LeaveWorkflow> findByApprovedBy_Id(Long userId);

    List<LeaveWorkflow> findByRejectedBy_Id(Long userId);

    Optional<LeaveWorkflow> findByLeaveApplication(LeaveApplication leaveApplication);

    void deleteByApprovedByOrRejectedBy(User approvedBy, User rejectedBy);
}