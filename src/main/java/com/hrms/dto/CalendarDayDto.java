package com.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// One cell in the admin work-calendar month grid.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDayDto {
    private LocalDate date;
    private String dayType; // WORKING / WEEKLY_OFF / PUBLIC_HOLIDAY / SPECIAL_WORKING_DAY
    private String label;
    private String source;
}
