package com.hrms.repository;

import com.hrms.entity.Shift;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShiftRepository extends MongoRepository<Shift, String> {
    Optional<Shift> findByName(String name);
}
