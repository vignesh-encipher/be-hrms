package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "expense_claims")
public class ExpenseClaim {
    @Id
    private String id;

    private String employeeId;
    private String employeeName;

    private String category; // Travel, Food, Internet, Telephone, Fuel, Medical, Office Expenses, Client Entertainment, Training, Certification, Other
    private Double amount;
    private LocalDate expenseDate;
    private String costCentre;
    private String description;

    @Builder.Default
    private List<String> receiptFilePaths = new ArrayList<>();

    private String status; // PENDING, IN_PROGRESS, APPROVED, REJECTED, PAID

    private Integer totalLevels; // 1 or 2
    private Integer currentLevel; // 1 or 2

    private String level1ApproverId;
    private String level1ApproverName;
    private String level1Role; // e.g. MANAGER
    private String level1Status; // Pending, Approved, Rejected
    private String level1Remarks;

    private String level2ApproverId;
    private String level2ApproverName;
    private String level2Role; // e.g. FINANCE
    private String level2Status; // Pending, Approved, Rejected
    private String level2Remarks;

    private String policyBreach; // nullable, e.g. "Certification cap of Rs.15000/year exceeded"

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<ApprovalAuditLog> auditLogs = new ArrayList<>();
}
