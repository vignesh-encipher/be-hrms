package com.hrms.serviceImpl;

import com.hrms.entity.Department;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.DepartmentRepository;
import com.hrms.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Override
    public Department getDepartmentById(String id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    @Override
    public Department createDepartment(Department department) {
        return departmentRepository.save(department);
    }

    @Override
    public Department updateDepartment(String id, Department departmentDetails) {
        Department department = getDepartmentById(id);
        department.setName(departmentDetails.getName());
        department.setCode(departmentDetails.getCode());
        department.setDescription(departmentDetails.getDescription());
        return departmentRepository.save(department);
    }

    @Override
    public void deleteDepartment(String id) {
        Department department = getDepartmentById(id);
        departmentRepository.delete(department);
    }
}
