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
@Document(collection = "leave_requests")
public class LeaveRequest {
    @Id
    private String id;
    
    private String employeeId;
    private String leaveType; // Casual Leave, Sick Leave, Paid Leave, Maternity Leave, Loss of Pay
    private LocalDate startDate;
    private LocalDate endDate;
    private Double numberOfDays;
    private String reason;
    
    private String status; // Pending, Approved, Rejected
    private String managerStatus; // Pending, Approved, Rejected
    private String hrStatus; // Pending, Approved, Rejected
    
    private String managerRemarks;
    private String hrRemarks;
}
