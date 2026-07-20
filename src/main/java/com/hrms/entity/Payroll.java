package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payroll")
public class Payroll {
    @Id
    private String id;
    
    private String employeeId;
    private String month; // e.g. "January" or "01"
    private int year;
    
    private Double basic;
    private Double hra;
    private Double allowance;
    private Double bonus;
    private Double deductions;
    private Double netSalary;
    
    private String status; // Generated, Paid, Processing
    private LocalDate paidDate;
}
