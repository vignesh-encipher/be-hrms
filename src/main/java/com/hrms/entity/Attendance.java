package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One document per employeeId + date. Replaces the old multi-document-per-day
 * clockIn/clockOut model with a single document holding a list of sessions
 * (check-in/check-out pairs) and breaks for that day.
 *
 * Allowed values for {@code status} (plain String, documented here since this
 * codebase does not use enums for status fields):
 *   Present, Absent, Late, EarlyCheckout, HalfDay, ShortHours, FullDay,
 *   Overtime, OnBreak, Working, CheckedOut, MissingCheckIn, MissingCheckOut,
 *   Holiday, WeeklyOff, Leave
 *
 * NOTE: `status` carries the single dominant word for the day (e.g. Present /
 * Absent / HalfDay / ShortHours). Late / early-checkout / overtime are NOT
 * folded into status - they are exposed as separate numeric fields
 * (lateMinutes / earlyCheckoutMinutes / overtimeMinutes) so the frontend can
 * show both together, e.g. "Present, 12m late".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "attendance")
public class Attendance {
    @Id
    private String id;

    private String employeeId;
    private LocalDate date;
    private String shiftId;

    @Builder.Default
    private List<AttendanceSession> sessions = new ArrayList<>();

    @Builder.Default
    private List<AttendanceBreak> breaks = new ArrayList<>();

    private Long totalDurationMinutes;
    private Long totalBreakMinutes;
    private Long effectiveWorkingMinutes;
    private Long overtimeMinutes;
    private Long lateMinutes;
    private Long earlyCheckoutMinutes;

    private String status;
    private String remarks;

    // Set once per day (first touched at check-in, or lazily on read) from
    // WorkCalendarService.resolveDayType(): WORKING / WEEKLY_OFF / PUBLIC_HOLIDAY /
    // SPECIAL_WORKING_DAY / LEAVE. Drives the weekend-work-policy handling in
    // recomputeAggregates() (see AttendanceServiceImpl).
    private String dayType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
