package com.hrms.repository;

import com.hrms.entity.Requisition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequisitionRepository extends MongoRepository<Requisition, String> {
    List<Requisition> findByRaisedByEmployeeId(String raisedByEmployeeId);
    List<Requisition> findByStatus(String status);
    long countByStatus(String status);
}
