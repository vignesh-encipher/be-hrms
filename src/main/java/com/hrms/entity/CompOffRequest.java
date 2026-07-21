package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "compoff_requests")
public class CompOffRequest {
    @Id
    private String id;
    
    private String employeeId;
    private LocalDate requestDate;
    private LocalDate workedDate;
    private String startTime;          // e.g. "09:00"
    private String endTime;            // e.g. "17:00"
    private Double hoursWorked;        // e.g. 8.0
    private Double earnedDays;         // e.g. 1.0
    private String reason;
    private String attachmentUrl;
    
    private String status;            // Pending, Approved, Rejected, Expired, Used
    private LocalDate expiryDate;
    private String approvedBy;
    private String remarks;
}
