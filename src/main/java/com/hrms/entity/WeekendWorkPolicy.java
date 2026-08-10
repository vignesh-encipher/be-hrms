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
 * Org-level policy governing what happens when an employee works on a day
 * resolved as WEEKLY_OFF. Simple current-policy model - one active row at a
 * time (no history browsing UI is required for this, unlike WorkCalendarRule).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "weekend_work_policies")
public class WeekendWorkPolicy {
    @Id
    private String id;

    @Builder.Default
    private String treatAs = "OVERTIME"; // OVERTIME / NORMAL_HOURS / COMPENSATORY_OFF

    @Builder.Default
    private boolean requiresManagerApproval = false;

    @Builder.Default
    private boolean requiresHrApproval = false;

    private LocalDate effectiveFrom;

    private String createdBy;
    private LocalDateTime createdAt;

    @Builder.Default
    private boolean active = true;
}
