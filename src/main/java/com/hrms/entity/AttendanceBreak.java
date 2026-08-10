package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Embedded within Attendance.breaks - one break start/end pair.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceBreak {
    private LocalDateTime startTime;
    private LocalDateTime endTime; // null while the break is still open
}
