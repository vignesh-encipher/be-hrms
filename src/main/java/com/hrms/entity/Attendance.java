package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;

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
    private LocalTime clockIn;
    private LocalTime clockOut;
    private String status; // Present, Absent, Half Day, WFH, Holiday
    private String remarks;
}
