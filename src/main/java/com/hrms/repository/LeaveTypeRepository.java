package com.hrms.repository;

import com.hrms.entity.LeaveType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaveTypeRepository extends MongoRepository<LeaveType, String> {
    Optional<LeaveType> findByCode(String code);
}
