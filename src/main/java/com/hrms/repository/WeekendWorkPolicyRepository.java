package com.hrms.repository;

import com.hrms.entity.WeekendWorkPolicy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeekendWorkPolicyRepository extends MongoRepository<WeekendWorkPolicy, String> {
    List<WeekendWorkPolicy> findByActiveTrue();
}
