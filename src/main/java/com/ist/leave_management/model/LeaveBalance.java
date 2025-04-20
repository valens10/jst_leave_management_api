package com.ist.leave_management.model;

import com.ist.user_management.model.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tb_leave_balance", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "leave_type_id" }))
public class LeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private Integer balance;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // New fields for auto-accrual and carry-forward
    @Column(name = "last_accrual_date")
    private LocalDate lastAccrualDate;

    @Column(name = "carried_forward_days")
    private Integer carriedForwardDays = 0;

    @Column(name = "expired_days")
    private Integer expiredDays = 0;

    @Column(name = "yearly_accrued_days")
    private Integer yearlyAccruedDays = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (validUntil == null) {
            // Set to last day of current year
            validUntil = LocalDate.now().withMonth(12).withDayOfMonth(31);
        }
        if (lastAccrualDate == null) {
            lastAccrualDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}