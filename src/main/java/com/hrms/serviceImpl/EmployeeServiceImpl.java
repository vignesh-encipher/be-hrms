package com.hrms.serviceImpl;

import com.hrms.dto.EmployeeDto;
import com.hrms.entity.Department;
import com.hrms.entity.Designation;
import com.hrms.entity.Employee;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.DepartmentRepository;
import com.hrms.repository.DesignationRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DesignationRepository designationRepository;

    private EmployeeDto convertToDto(Employee employee) {
        EmployeeDto dto = EmployeeDto.builder()
                .id(employee.getId())
                .employeeId(employee.getEmployeeId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .gender(employee.getGender())
                .dob(employee.getDob())
                .bloodGroup(employee.getBloodGroup())
                .departmentId(employee.getDepartmentId())
                .designationId(employee.getDesignationId())
                .managerId(employee.getManagerId())
                .joiningDate(employee.getJoiningDate())
                .employmentType(employee.getEmploymentType())
                .salary(employee.getSalary())
                .address(employee.getAddress())
                .emergencyContact(employee.getEmergencyContact())
                .status(employee.getStatus())
                .photo(employee.getPhoto())
                .userId(employee.getUserId())
                .build();

        // Resolve names
        if (employee.getDepartmentId() != null) {
            departmentRepository.findById(employee.getDepartmentId())
                    .ifPresent(dept -> dto.setDepartmentName(dept.getName()));
        }
        if (employee.getDesignationId() != null) {
            designationRepository.findById(employee.getDesignationId())
                    .ifPresent(desg -> dto.setDesignationTitle(desg.getTitle()));
        }
        if (employee.getManagerId() != null) {
            employeeRepository.findByEmployeeId(employee.getManagerId())
                    .ifPresent(mgr -> dto.setManagerName(mgr.getFirstName() + " " + mgr.getLastName()));
        }

        return dto;
    }

    private Employee convertToEntity(EmployeeDto dto) {
        return Employee.builder()
                .id(dto.getId())
                .employeeId(dto.getEmployeeId())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .gender(dto.getGender())
                .dob(dto.getDob())
                .bloodGroup(dto.getBloodGroup())
                .departmentId(dto.getDepartmentId())
                .designationId(dto.getDesignationId())
                .managerId(dto.getManagerId())
                .joiningDate(dto.getJoiningDate())
                .employmentType(dto.getEmploymentType())
                .salary(dto.getSalary())
                .address(dto.getAddress())
                .emergencyContact(dto.getEmergencyContact())
                .status(dto.getStatus())
                .photo(dto.getPhoto())
                .userId(dto.getUserId())
                .build();
    }

    @Override
    public Page<EmployeeDto> getAllEmployees(String search, Pageable pageable) {
        Page<Employee> page;
        if (search != null && !search.isEmpty()) {
            page = employeeRepository.searchEmployees(search, pageable);
        } else {
            page = employeeRepository.findAll(pageable);
        }
        return page.map(this::convertToDto);
    }

    @Override
    public List<EmployeeDto> getAllEmployeesList() {
        return employeeRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public EmployeeDto getEmployeeById(String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return convertToDto(employee);
    }

    @Override
    public EmployeeDto getEmployeeByEmployeeId(String employeeId) {
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with employeeId: " + employeeId));
        return convertToDto(employee);
    }

    @Override
    public EmployeeDto getEmployeeByUserId(String userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with userId: " + userId));
        return convertToDto(employee);
    }

    @Override
    public EmployeeDto createEmployee(EmployeeDto employeeDto) {
        Employee employee = convertToEntity(employeeDto);
        if (employee.getEmployeeId() == null || employee.getEmployeeId().isEmpty()) {
            long count = employeeRepository.count();
            employee.setEmployeeId(String.format("EMP-%03d", count + 1));
        }
        Employee saved = employeeRepository.save(employee);
        return convertToDto(saved);
    }

    @Override
    public EmployeeDto updateEmployee(String id, EmployeeDto employeeDto) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        
        existing.setFirstName(employeeDto.getFirstName());
        existing.setLastName(employeeDto.getLastName());
        existing.setEmail(employeeDto.getEmail());
        existing.setPhone(employeeDto.getPhone());
        existing.setGender(employeeDto.getGender());
        existing.setDob(employeeDto.getDob());
        existing.setBloodGroup(employeeDto.getBloodGroup());
        existing.setDepartmentId(employeeDto.getDepartmentId());
        existing.setDesignationId(employeeDto.getDesignationId());
        existing.setManagerId(employeeDto.getManagerId());
        existing.setJoiningDate(employeeDto.getJoiningDate());
        existing.setEmploymentType(employeeDto.getEmploymentType());
        existing.setSalary(employeeDto.getSalary());
        existing.setAddress(employeeDto.getAddress());
        existing.setEmergencyContact(employeeDto.getEmergencyContact());
        existing.setStatus(employeeDto.getStatus());
        if (employeeDto.getPhoto() != null) {
            existing.setPhoto(employeeDto.getPhoto());
        }

        Employee saved = employeeRepository.save(existing);
        return convertToDto(saved);
    }

    @Override
    public void deleteEmployee(String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
    }
}
