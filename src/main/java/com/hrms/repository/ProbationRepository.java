package com.hrms.repository;

import com.hrms.entity.ProbationRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProbationRepository extends MongoRepository<ProbationRecord, String> {
    List<ProbationRecord> findByStatus(String status);
    List<ProbationRecord> findByEmployeeId(String employeeId);
    List<ProbationRecord> findByConfirmationDueDateBefore(LocalDate date);
    List<ProbationRecord> findByConfirmationDueDateBetween(LocalDate start, LocalDate end);
}
