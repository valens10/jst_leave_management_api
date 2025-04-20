package com.ist.leave_management.service;

import com.ist.leave_management.model.LeaveBalance;
import com.ist.leave_management.model.LeaveType;
import com.ist.leave_management.repository.LeaveBalanceRepository;
import com.ist.leave_management.repository.LeaveTypeRepository;
import com.ist.user_management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveBalanceManagementService {
    private static final Logger logger = LoggerFactory.getLogger(LeaveBalanceManagementService.class);
    private static final double MONTHLY_ACCRUAL_RATE = 1.66;
    private static final int MAX_CARRY_FORWARD_DAYS = 5;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Process monthly accrual for all annual leave balances
     * Runs on the 1st of each month at 00:00
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void processMonthlyAccrual() {
        logger.info("Starting monthly leave accrual process");
        LocalDate today = LocalDate.now();

        // Get all annual leave types
        List<LeaveType> annualLeaveTypes = leaveTypeRepository.findByIsAnnualLeave(true);

        for (LeaveType leaveType : annualLeaveTypes) {
            // Get all leave balances for this leave type
            List<LeaveBalance> balances = leaveBalanceRepository.findByLeaveType(leaveType);

            for (LeaveBalance balance : balances) {
                try {
                    // Check if accrual is needed (skip if already accrued this month)
                    if (balance.getLastAccrualDate() != null &&
                            balance.getLastAccrualDate().getMonth() == today.getMonth() &&
                            balance.getLastAccrualDate().getYear() == today.getYear()) {
                        continue;
                    }

                    // Add monthly accrual
                    double newBalance = balance.getBalance() + MONTHLY_ACCRUAL_RATE;
                    balance.setBalance((int) newBalance);
                    balance.setYearlyAccruedDays(balance.getYearlyAccruedDays() + (int) MONTHLY_ACCRUAL_RATE);
                    balance.setLastAccrualDate(today);

                    leaveBalanceRepository.save(balance);

                    logger.info("Accrued {} days for user {} (new balance: {})",
                            MONTHLY_ACCRUAL_RATE,
                            balance.getUser().getEmail(),
                            newBalance);
                } catch (Exception e) {
                    logger.error("Error processing accrual for user {}: {}",
                            balance.getUser().getEmail(), e.getMessage());
                }
            }
        }
        logger.info("Completed monthly leave accrual process");
    }

    /**
     * Process year-end carry forward for all annual leave balances
     * Runs on December 31st at 23:59
     */
    @Scheduled(cron = "0 59 23 31 12 ?")
    @Transactional
    public void processYearEndCarryForward() {
        logger.info("Starting year-end leave carry forward process");
        LocalDate today = LocalDate.now();
        LocalDate nextYear = today.plusYears(1);

        // Get all annual leave types
        List<LeaveType> annualLeaveTypes = leaveTypeRepository.findByIsAnnualLeave(true);

        for (LeaveType leaveType : annualLeaveTypes) {
            // Get all leave balances for this leave type
            List<LeaveBalance> balances = leaveBalanceRepository.findByLeaveType(leaveType);

            for (LeaveBalance balance : balances) {
                try {
                    int currentBalance = balance.getBalance();

                    // Calculate carry forward (max 5 days)
                    int carryForward = Math.min(currentBalance, MAX_CARRY_FORWARD_DAYS);
                    int expiredDays = currentBalance - carryForward;

                    // Update the balance
                    balance.setCarriedForwardDays(carryForward);
                    balance.setExpiredDays(expiredDays);
                    balance.setBalance(carryForward); // Reset balance to carried forward amount
                    balance.setYearlyAccruedDays(0); // Reset yearly accrued days
                    balance.setValidUntil(nextYear.withMonth(12).withDayOfMonth(31));

                    leaveBalanceRepository.save(balance);

                    // Send notification to user
                    notificationService.sendLeaveBalanceCarryForwardNotification(
                            balance.getUser(),
                            carryForward,
                            expiredDays);

                    logger.info("Processed carry forward for user {}: carried forward={}, expired={}",
                            balance.getUser().getEmail(),
                            carryForward,
                            expiredDays);
                } catch (Exception e) {
                    logger.error("Error processing carry forward for user {}: {}",
                            balance.getUser().getEmail(), e.getMessage());
                }
            }
        }
        logger.info("Completed year-end leave carry forward process");
    }

    /**
     * Initialize leave balance for a new user
     */
    @Transactional
    public void initializeLeaveBalance(Long userId, Long leaveTypeId) {
        LeaveBalance balance = new LeaveBalance();
        balance.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found")));
        balance.setLeaveType(leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new RuntimeException("Leave type not found")));
        balance.setBalance(0);
        balance.setYearlyAccruedDays(0);
        balance.setCarriedForwardDays(0);
        balance.setExpiredDays(0);
        balance.setLastAccrualDate(LocalDate.now());

        leaveBalanceRepository.save(balance);
        logger.info("Initialized leave balance for user {} and leave type {}", userId, leaveTypeId);
    }
}