package com.hrms.repository;

import com.hrms.entity.JobOpening;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobOpeningRepository extends MongoRepository<JobOpening, String> {
    List<JobOpening> findByStatus(String status);
    List<JobOpening> findByDepartment(String department);
}
