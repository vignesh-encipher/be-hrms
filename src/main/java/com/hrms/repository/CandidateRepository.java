package com.hrms.repository;

import com.hrms.entity.Candidate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateRepository extends MongoRepository<Candidate, String> {
    List<Candidate> findByJobId(String jobId);
    List<Candidate> findByStage(String stage);
    List<Candidate> findByJobIdAndStage(String jobId, String stage);
}
