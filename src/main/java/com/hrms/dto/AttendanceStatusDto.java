package com.hrms.dto;

import com.hrms.entity.Attendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Live "today" status DTO. Working-minutes-so-far is always computed
 * server-side against the current server time - never trust a client value.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceStatusDto {
    private Attendance attendance;
    private String currentStatus; // Working, OnBreak, CheckedOut, NotCheckedIn
    private boolean checkedIn;
    private boolean onBreak;
    private long workingMinutesSoFar;
    private long breakMinutesSoFar;
    private long requiredWorkingMinutes;
    private long remainingMinutes;
}
