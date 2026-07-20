package com.hrms.repository;

import com.hrms.entity.LeaveRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends MongoRepository<LeaveRequest, String> {
    List<LeaveRequest> findByEmployeeId(String employeeId);
    List<LeaveRequest> findByStatus(String status);
    long countByStatus(String status);
}
