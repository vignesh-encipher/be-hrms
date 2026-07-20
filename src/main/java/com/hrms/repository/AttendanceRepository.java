package com.hrms.repository;

import com.hrms.entity.Attendance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends MongoRepository<Attendance, String> {
    List<Attendance> findByEmployeeIdAndDate(String employeeId, LocalDate date);
    List<Attendance> findByEmployeeIdAndDateBetween(String employeeId, LocalDate startDate, LocalDate endDate);
    List<Attendance> findByDate(LocalDate date);
    long countByDateAndStatus(LocalDate date, String status);
}
