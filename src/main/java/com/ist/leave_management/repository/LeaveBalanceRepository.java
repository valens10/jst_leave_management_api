package com.ist.leave_management.repository;

import com.ist.leave_management.model.LeaveBalance;
import com.ist.leave_management.model.LeaveType;
import com.ist.user_management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByUserId(Long userId);

    Optional<LeaveBalance> findByUserIdAndLeaveTypeId(Long userId, Long leaveTypeId);

    List<LeaveBalance> findByValidUntilBefore(LocalDate date);

    List<LeaveBalance> findByUser(User user);

    void deleteByUser(User user);

    List<LeaveBalance> findByLeaveType(LeaveType leaveType);
}