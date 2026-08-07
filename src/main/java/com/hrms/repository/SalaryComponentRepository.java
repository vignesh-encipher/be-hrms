package com.hrms.repository;

import com.hrms.entity.SalaryComponent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalaryComponentRepository extends MongoRepository<SalaryComponent, String> {
}
