package com.ist.leave_management.repository;

import com.ist.leave_management.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    Optional<Holiday> findByName(String name);

    List<Holiday> findByDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByName(String name);
}