package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "salary_components")
public class SalaryComponent {
    @Id
    private String id;

    private String name;
    private String type; // Earning / Deduction
    private String calculationDescription; // e.g. "12% of Basic capped at 15000"
    private String appliesTo; // e.g. "All Employees" or department name
    private boolean isStatutory;
    private boolean isPartOfCtc;
}
