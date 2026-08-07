package com.hrms.repository;

import com.hrms.entity.PreOnboardingCandidate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreOnboardingRepository extends MongoRepository<PreOnboardingCandidate, String> {
    List<PreOnboardingCandidate> findByStatus(String status);
    List<PreOnboardingCandidate> findByEmail(String email);
}
