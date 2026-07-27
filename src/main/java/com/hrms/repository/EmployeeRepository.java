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
    @Query("{ 'employeeId': ?0, 'deleted': { $ne: true } }")
    Optional<Employee> findByEmployeeId(String employeeId);

    @Query("{ 'email': ?0, 'deleted': { $ne: true } }")
    Optional<Employee> findByEmail(String email);

    @Query(value = "{ 'email': ?0, 'deleted': { $ne: true } }", exists = true)
    Boolean existsByEmail(String email);

    @Query(value = "{ 'employeeId': ?0, 'deleted': { $ne: true } }", exists = true)
    Boolean existsByEmployeeId(String employeeId);

    @Query("{ 'managerId': ?0, 'deleted': { $ne: true } }")
    List<Employee> findByManagerId(String managerId);
    
    @Query("{ 'deleted': { $ne: true }, '$or': [ { 'firstName': { $regex: ?0, $options: 'i' } }, { 'lastName': { $regex: ?0, $options: 'i' } }, { 'employeeId': { $regex: ?0, $options: 'i' } }, { 'email': { $regex: ?0, $options: 'i' } } ] }")
    Page<Employee> searchEmployees(String query, Pageable pageable);
    
    @Query("{ 'departmentId': ?0, 'deleted': { $ne: true } }")
    Page<Employee> findByDepartmentId(String departmentId, Pageable pageable);
    
    @Query(value = "{ 'status': ?0, 'deleted': { $ne: true } }", count = true)
    long countByStatus(String status);

    @Query("{ 'deleted': { $ne: true } }")
    List<Employee> findAllActive();

    @Query("{ 'deleted': { $ne: true } }")
    Page<Employee> findAllActive(Pageable pageable);

    @Query(value = "{ 'deleted': { $ne: true } }", count = true)
    long countActive();
}
