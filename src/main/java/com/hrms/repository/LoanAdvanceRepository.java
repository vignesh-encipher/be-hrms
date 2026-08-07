package com.hrms.repository;

import com.hrms.entity.LoanAdvance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanAdvanceRepository extends MongoRepository<LoanAdvance, String> {
    List<LoanAdvance> findByEmployeeId(String employeeId);
}
