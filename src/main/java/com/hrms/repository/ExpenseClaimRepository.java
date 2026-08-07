package com.hrms.repository;

import com.hrms.entity.ExpenseClaim;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseClaimRepository extends MongoRepository<ExpenseClaim, String> {
    List<ExpenseClaim> findByEmployeeId(String employeeId);
    List<ExpenseClaim> findByStatus(String status);
    long countByStatus(String status);
}
