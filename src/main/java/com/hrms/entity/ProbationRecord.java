package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "probation_records")
public class ProbationRecord {
    @Id
    private String id;

    private String employeeId;
    private String employeeName;
    private String designation;

    private LocalDate dateOfJoining;

    @Builder.Default
    private int probationMonths = 6;

    private LocalDate confirmationDueDate;

    // Upcoming, Manager Review, Confirmed, Probation Extended, Terminated
    private String status;

    @Builder.Default
    private List<RatingItem> ratings = new java.util.ArrayList<>();

    private String decisionRemarks;
    private LocalDate decisionDate;

    // Pending, Notified
    private String payrollNotificationStatus;

    @Builder.Default
    private List<ApprovalAuditLog> auditLogs = new java.util.ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingItem {
        private String criterion;
        private int score; // 1-5
    }
}
