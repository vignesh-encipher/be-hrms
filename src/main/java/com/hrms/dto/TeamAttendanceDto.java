package com.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamAttendanceDto {
    private String employeeId;
    private String employeeName;
    private String status;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private Long workingMinutes;
}
