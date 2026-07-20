package com.hrms.repository;

import com.hrms.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends MongoRepository<Employee, String> {
    Optional<Employee> findByEmployeeId(String employeeId);
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByUserId(String userId);
    List<Employee> findByManagerId(String managerId);
    
    @Query("{ '$or': [ { 'firstName': { $regex: ?0, $options: 'i' } }, { 'lastName': { $regex: ?0, $options: 'i' } }, { 'employeeId': { $regex: ?0, $options: 'i' } }, { 'email': { $regex: ?0, $options: 'i' } } ] }")
    Page<Employee> searchEmployees(String query, Pageable pageable);
    
    Page<Employee> findByDepartmentId(String departmentId, Pageable pageable);
    
    long countByStatus(String status);
}
