package com.hrms.service;

import com.hrms.entity.Department;
import java.util.List;

public interface DepartmentService {
    List<Department> getAllDepartments();
    Department getDepartmentById(String id);
    Department createDepartment(Department department);
    Department updateDepartment(String id, Department department);
    void deleteDepartment(String id);
}
