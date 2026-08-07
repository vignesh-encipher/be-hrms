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
@Document(collection = "separation_requests")
public class SeparationRequest {
    @Id
    private String id;

    private String employeeId;
    private String employeeName;
    private String role;
    private String department;

    private LocalDate resignationDate;
    private LocalDate lastWorkingDay;
    private Integer noticePeriod; // days
    private String reason;

    // Employee Review, Manager Review, HR Review, Department Head Review, F&F Pending, Closed, Rejected
    private String status;

    @Builder.Default
    private List<Clearance> clearances = new ArrayList<>();

    @Builder.Default
    private List<AssetReturn> assetReturns = new ArrayList<>();

    private FullAndFinal fullAndFinal;

    @Builder.Default
    private List<ApprovalAuditLog> auditLogs = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Clearance {
        private String department; // HR, IT, Admin, Finance
        private boolean done;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetReturn {
        private String assetTag;
        private String description;
        private boolean returned;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FullAndFinal {
        private Double salaryTillLwd;
        private Double leaveEncashment;
        private Double gratuity;
        private Double recoveries;
        private Double netPayable;
    }
}
