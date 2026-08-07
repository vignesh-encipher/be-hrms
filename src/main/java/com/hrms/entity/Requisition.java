package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "requisitions")
public class Requisition {
    @Id
    private String id;

    private String roleTitle;
    private String department;
    private String requestType; // New Position, Replacement, Contract, Intern, Consultant
    private Integer numberOfPositions;
    private String budgetedCtcMin;
    private String budgetedCtcMax;
    private String businessUnit;
    private String workLocation;
    private String grade;
    private String minimumExperience;
    private List<String> requiredSkills;
    private String businessJustification;

    private String raisedByEmployeeId;
    private String raisedByName;
    private LocalDate raisedDate;
    private LocalDate targetJoiningDate;

    private String status; // Pending Level 1 - Reporting Manager, Pending Level 2 - Department Head, ..., Approved, Rejected

    private Integer totalLevels; // up to 5
    private Integer currentLevel;

    private String level1ApproverId;
    private String level1ApproverName;
    private String level1Role; // MANAGER
    private String level1Status;
    private String level1Remarks;

    private String level2ApproverId;
    private String level2ApproverName;
    private String level2Role; // Department Head (MANAGER)
    private String level2Status;
    private String level2Remarks;

    private String level3ApproverId;
    private String level3ApproverName;
    private String level3Role; // FINANCE
    private String level3Status;
    private String level3Remarks;

    private String level4ApproverId;
    private String level4ApproverName;
    private String level4Role; // HR
    private String level4Status;
    private String level4Remarks;

    private String level5ApproverId;
    private String level5ApproverName;
    private String level5Role; // Management (SUPER_ADMIN)
    private String level5Status;
    private String level5Remarks;

    @Builder.Default
    private List<ApprovalAuditLog> auditLogs = new ArrayList<>();
}
