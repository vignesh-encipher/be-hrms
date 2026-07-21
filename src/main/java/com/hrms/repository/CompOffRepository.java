package com.hrms.repository;

import com.hrms.entity.CompOffRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompOffRepository extends MongoRepository<CompOffRequest, String> {
    List<CompOffRequest> findByEmployeeId(String employeeId);
    List<CompOffRequest> findByStatus(String status);
}
