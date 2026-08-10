package com.hrms.repository;

import com.hrms.entity.SpecialDateOverride;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SpecialDateOverrideRepository extends MongoRepository<SpecialDateOverride, String> {
    List<SpecialDateOverride> findByDate(LocalDate date);
    List<SpecialDateOverride> findByDateBetween(LocalDate from, LocalDate to);
    List<SpecialDateOverride> findByAppliesToScopeAndAppliesToRefIdAndDateBetween(
            String appliesToScope, String appliesToRefId, LocalDate from, LocalDate to);
    List<SpecialDateOverride> findByAppliesToScopeAndDateBetween(String appliesToScope, LocalDate from, LocalDate to);
}
