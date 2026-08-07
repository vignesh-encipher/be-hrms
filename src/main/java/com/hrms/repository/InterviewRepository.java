package com.hrms.repository;

import com.hrms.entity.Interview;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewRepository extends MongoRepository<Interview, String> {
    List<Interview> findByCandidateId(String candidateId);
    List<Interview> findByStatus(String status);
}
