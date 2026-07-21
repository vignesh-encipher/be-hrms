package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalAuditLog {
    private String approverId;
    private String approverName;
    private String approverRole;
    private String action; // Approved, Rejected
    private LocalDateTime timestamp;
    private String comments;
    private Integer level; // 1 or 2
}
