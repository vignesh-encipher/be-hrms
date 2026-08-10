package com.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAttendanceSummaryDto {
    private String employeeId;
    private int month;
    private int year;
    private long workingDays;
    private long present;
    private long absent;
    private long leave;
    private long lateCount;
    private long earlyCheckoutCount;
    private long totalOvertimeMinutes;
    private double averageEffectiveWorkingMinutes;
}
