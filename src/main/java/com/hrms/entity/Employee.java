package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "employees")
public class Employee {
    @Id
    private String id;

    @Indexed(unique = true)
    private String employeeId; // e.g. EMP-001

    private String firstName;
    private String lastName;

    @Indexed(unique = true)
    private String email;

    private String phone;
    private String gender;
    private LocalDate dob;
    private String bloodGroup;
    
    private String departmentId;
    private String designationId;
    private String managerId; // employeeId of manager
    private String hrApproverId; // employeeId of HR approver

    private LocalDate joiningDate;
    private String employmentType; // Full Time, Part Time, Contract, Intern
    private Double salary;
    private String address;
    private String emergencyContact;
    private String status; // Active, Terminated, Resigned, On Leave
    private String photo; // base64 or URL

    // Reference to User ID for auth link
    private String userId;
}
