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
@Document(collection = "leave_requests")
public class LeaveRequest {
    @Id
    private String id;
    
    private String employeeId;
    private String leaveType; // Casual Leave, Sick Leave, Paid Leave, Maternity Leave, Loss of Pay
    private LocalDate startDate;
    private LocalDate endDate;
    private Double numberOfDays;
    private String reason;
    
    private String status; // Pending Level 1 - [Role], Pending Level 2 - [Role], Approved, Rejected
    
    private Integer totalLevels; // 1 or 2
    private Integer currentLevel; // 1 or 2
    
    private String level1ApproverId;
    private String level1ApproverName;
    private String level1Role; // e.g. MANAGER
    private String level1Status; // Pending, Approved, Rejected
    private String level1Remarks;

    private String level2ApproverId;
    private String level2ApproverName;
    private String level2Role; // e.g. HR
    private String level2Status; // Pending, Approved, Rejected
    private String level2Remarks;

    @Builder.Default
    private java.util.List<ApprovalAuditLog> auditLogs = new java.util.ArrayList<>();
}


