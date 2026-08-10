package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Config-change audit log for the Work Calendar module. Deliberately a
 * dedicated entity rather than reusing ApprovalAuditLog, which models an
 * approval chain (approverId/level/action=Approved|Rejected) - a different
 * shape from a plain "what config changed and who changed it" log.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "work_calendar_audit_logs")
public class WorkCalendarAuditLog {
    @Id
    private String id;

    private String entityType; // WORK_CALENDAR_RULE / SPECIAL_DATE_OVERRIDE / WEEKEND_POLICY
    private String entityId;
    private String action; // CREATED / UPDATED / DEACTIVATED

    private String previousValue;
    private String newValue;

    private String changedBy;
    private LocalDateTime changedAt;
    private String reason;
}
