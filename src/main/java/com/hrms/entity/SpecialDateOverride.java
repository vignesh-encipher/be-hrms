package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Highest-priority per-date override in the Work Calendar resolution chain
 * (after approved leave, before Holiday collection and WorkCalendarRule).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "special_date_overrides")
public class SpecialDateOverride {
    @Id
    private String id;

    private LocalDate date;
    private String dayType; // WORKING / OFF / HOLIDAY
    private String reason;

    private String appliesToScope; // ORGANIZATION / LOCATION / DEPARTMENT / EMPLOYEE_GROUP / EMPLOYEE
    private String appliesToRefId; // nullable for ORGANIZATION

    private String createdBy;
    private LocalDateTime createdAt;
}
