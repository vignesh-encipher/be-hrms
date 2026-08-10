package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Allowed values for {@code action}:
 *   CHECK_IN, CHECK_OUT, BREAK_START, BREAK_END, CORRECTION_REQUESTED,
 *   CORRECTION_APPROVED, CORRECTION_REJECTED, AUTO_CLOSE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "attendance_audit_logs")
public class AttendanceAuditLog {
    @Id
    private String id;

    private String employeeId;
    private String attendanceId;
    private String action;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String userAgent;
    private String previousValue;
    private String newValue;
    private String reason;
    private String performedBy; // employeeId, or "SYSTEM"
}
