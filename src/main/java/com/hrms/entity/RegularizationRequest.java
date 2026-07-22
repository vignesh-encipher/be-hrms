package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "regularizations")
public class RegularizationRequest {
    @Id
    private String id;
    
    private String employeeId;
    private LocalDate attendanceDate;
    private String requestType; // Missed Check-In, Missed Check-Out, Incorrect Punch, Full Day Regularization, Half Day Regularization
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String reason;
    
    private String status; // Pending, Approved, Rejected
    private String managerId; // L1 Manager employeeId
    
    private String remarks; // Approver comments
    private LocalDateTime submittedOn;
    private String approverName;
    private LocalDateTime approvedOrRejectedOn;
}
