package com.hrms.repository;

import com.hrms.entity.RegularizationRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegularizationRepository extends MongoRepository<RegularizationRequest, String> {
    List<RegularizationRequest> findByEmployeeId(String employeeId);
    List<RegularizationRequest> findByManagerId(String managerId);
    List<RegularizationRequest> findByManagerIdAndStatus(String managerId, String status);
}
