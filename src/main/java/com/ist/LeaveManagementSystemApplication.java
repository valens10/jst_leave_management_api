package com.ist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = {
        "com.ist",
        "com.ist.leave_management",
        "com.ist.config",
        "com.ist.common"
})
@EnableJpaAuditing
public class LeaveManagementSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeaveManagementSystemApplication.class, args);
    }
}