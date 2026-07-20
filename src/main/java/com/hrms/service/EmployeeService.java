package com.hrms.service;

import com.hrms.dto.EmployeeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeService {
    Page<EmployeeDto> getAllEmployees(String search, Pageable pageable);
    List<EmployeeDto> getAllEmployeesList();
    EmployeeDto getEmployeeById(String id);
    EmployeeDto getEmployeeByEmployeeId(String employeeId);
    EmployeeDto getEmployeeByUserId(String userId);
    EmployeeDto createEmployee(EmployeeDto employeeDto);
    EmployeeDto updateEmployee(String id, EmployeeDto employeeDto);
    void deleteEmployee(String id);
}
