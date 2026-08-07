package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tickets")
public class Ticket {
    @Id
    private String id;

    private String subject;
    private String description;
    private String category; // HR, IT, Payroll, Finance, Admin
    private String raisedByEmployeeId;
    private String raisedByName;
    private String priority; // Low, Medium, High
    private Integer slaHours; // derived from priority
    private String status; // Open, In progress, Resolved
    private String assignedToEmployeeId;
    private String assignedToName;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    @Builder.Default
    private List<TicketReply> replies = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketReply {
        private String authorId;
        private String authorName;
        private String message;
        private LocalDateTime timestamp;
    }
}
