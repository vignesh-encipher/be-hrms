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
@Document(collection = "loan_advances")
public class LoanAdvance {
    @Id
    private String id;

    private String employeeId;
    private String kind; // "Salary advance" / "Personal loan"
    private Double amount;
    private Double emi;
    private int totalInstalments;
    private int instalmentsPaid;
    private Double outstanding;
    private String status; // "Pending approval" / "Recovering" / "Closed"
}
