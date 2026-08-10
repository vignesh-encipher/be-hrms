package com.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Result of WorkCalendarService.resolveDayType(employeeId, date).
 * dayType: WORKING / WEEKLY_OFF / PUBLIC_HOLIDAY / SPECIAL_WORKING_DAY / LEAVE
 * source: which resolver step produced the result (LEAVE / SPECIAL_DATE_OVERRIDE /
 *         HOLIDAY / WORK_CALENDAR_RULE:<scope> / SYSTEM_DEFAULT)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayTypeResultDto {
    private LocalDate date;
    private String employeeId;
    private String dayType;
    private String reason;
    private String source;
}
