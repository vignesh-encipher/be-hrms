package com.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {
    private String id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dob;
    private String bloodGroup;
    
    private String departmentId;
    private String departmentName; // Resolved name
    
    private String designationId;
    private String designationTitle; // Resolved title
    
    private String managerId;
    private String managerName; // Resolved full name
    
    private LocalDate joiningDate;
    private LocalDate resignationDate;
    private String employmentType;
    private Double salary;
    private String address;
    private String emergencyContact;
    private String status;
    private String photo;
    private String userId;
    
    // Account creation fields
    private String password;
    private String role;
}

