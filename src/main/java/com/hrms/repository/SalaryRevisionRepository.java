package com.hrms.repository;

import com.hrms.entity.SalaryRevision;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryRevisionRepository extends MongoRepository<SalaryRevision, String> {
    List<SalaryRevision> findByEmployeeId(String employeeId);
}
