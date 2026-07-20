package com.hrms.repository;

import com.hrms.entity.Designation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignationRepository extends MongoRepository<Designation, String> {
    List<Designation> findByDepartmentId(String departmentId);
}
