package com.hrms.repository;

import com.hrms.entity.PayrollRun;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRunRepository extends MongoRepository<PayrollRun, String> {
    Optional<PayrollRun> findByPeriod(String period);
    List<PayrollRun> findAll();
}
