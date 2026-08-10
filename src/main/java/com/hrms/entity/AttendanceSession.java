package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Embedded within Attendance.sessions - one check-in/check-out pair.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSession {
    private LocalDateTime checkIn;
    private LocalDateTime checkOut; // null while the session is still open
}
