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
@Document(collection = "salary_revisions")
public class SalaryRevision {
    @Id
    private String id;

    private String employeeId;
    private Double fromCtc;
    private Double toCtc;
    private LocalDate effectiveDate;
    private String reason;
    private String status; // "Pending approval" / "Approved"
    private double arrearsAmount;
}
