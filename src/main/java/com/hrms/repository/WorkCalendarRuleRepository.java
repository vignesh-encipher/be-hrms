package com.hrms.repository;

import com.hrms.entity.WorkCalendarRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkCalendarRuleRepository extends MongoRepository<WorkCalendarRule, String> {
    List<WorkCalendarRule> findByScopeAndScopeRefId(String scope, String scopeRefId);
    List<WorkCalendarRule> findByScope(String scope);
}
