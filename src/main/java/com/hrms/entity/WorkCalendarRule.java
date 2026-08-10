package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hierarchical weekly-pattern configuration for the Work Calendar / Weekly
 * Off engine. Plain-String fields per this codebase's convention (no enums).
 *
 * scope: ORGANIZATION / LOCATION / DEPARTMENT / SHIFT / EMPLOYEE
 * scopeRefId: null for ORGANIZATION scope, else the department/shift/employee/location id.
 *
 * weeklyPattern: day-of-week (0=Sunday..6=Saturday) -> "WORKING" / "OFF".
 *
 * saturdayPattern (nullable): only meaningful for day 6 (Saturday); when set it
 * OVERRIDES the flat Saturday entry in weeklyPattern for this rule. One of:
 *   EVERY_WORKING, EVERY_OFF, FIRST_THIRD_WORKING, SECOND_FOURTH_WORKING,
 *   FIRST_THIRD_OFF, CUSTOM
 *
 * Multiple rows can exist for the same scope+scopeRefId over time
 * (effective-dated). Never mutate an old row's dates - create a new row and
 * close out the old one's effectiveTo instead, to preserve history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "work_calendar_rules")
public class WorkCalendarRule {
    @Id
    private String id;

    private String scope; // ORGANIZATION / LOCATION / DEPARTMENT / SHIFT / EMPLOYEE
    private String scopeRefId; // null for ORGANIZATION

    @Builder.Default
    private Map<Integer, String> weeklyPattern = new LinkedHashMap<>();

    private String saturdayPattern; // nullable

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo; // null = still active

    private String createdBy;
    private LocalDateTime createdAt;

    @Builder.Default
    private boolean active = true;
}
