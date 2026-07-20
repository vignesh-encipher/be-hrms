package com.hrms.service;

import com.hrms.dto.EmployeeDto;
import com.hrms.entity.Employee;
import com.hrms.repository.DepartmentRepository;
import com.hrms.repository.DesignationRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.serviceImpl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DesignationRepository designationRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetEmployeeByIdSuccess() {
        Employee employee = Employee.builder()
                .id("1")
                .employeeId("EMP-001")
                .firstName("John")
                .lastName("Doe")
                .email("john@doe.com")
                .status("Active")
                .build();

        when(employeeRepository.findById("1")).thenReturn(Optional.of(employee));

        EmployeeDto result = employeeService.getEmployeeById("1");

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("EMP-001", result.getEmployeeId());
        verify(employeeRepository, times(1)).findById("1");
    }

    @Test
    void testCreateEmployeeAutoGeneratesId() {
        EmployeeDto request = EmployeeDto.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@smith.com")
                .build();

        Employee savedEntity = Employee.builder()
                .id("2")
                .employeeId("EMP-001")
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@smith.com")
                .build();

        when(employeeRepository.count()).thenReturn(0L);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEntity);

        EmployeeDto result = employeeService.createEmployee(request);

        assertNotNull(result);
        assertEquals("EMP-001", result.getEmployeeId());
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }
}
