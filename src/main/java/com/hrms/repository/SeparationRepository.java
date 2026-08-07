package com.hrms.repository;

import com.hrms.entity.SeparationRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeparationRepository extends MongoRepository<SeparationRequest, String> {
    List<SeparationRequest> findByEmployeeId(String employeeId);
    List<SeparationRequest> findByStatus(String status);
}
