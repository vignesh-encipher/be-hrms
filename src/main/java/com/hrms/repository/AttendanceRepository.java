package com.hrms.repository;

import com.hrms.entity.Attendance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends MongoRepository<Attendance, String> {
    Optional<Attendance> findByEmployeeIdAndDate(String employeeId, LocalDate date);
    List<Attendance> findByEmployeeIdAndDateBetween(String employeeId, LocalDate startDate, LocalDate endDate);
    List<Attendance> findByDate(LocalDate date);
    long countByDateAndStatus(LocalDate date, String status);

    // Docs whose date is before `date` and that still have at least one open session
    // (i.e. a session whose checkOut is null). Filtered further in-memory by the service.
    List<Attendance> findByDateBefore(LocalDate date);
}
