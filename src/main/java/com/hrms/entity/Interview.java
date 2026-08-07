package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "interviews")
public class Interview {
    @Id
    private String id;

    private String candidateId;
    private String roundName;
    private List<String> panel;
    private LocalDateTime scheduledAt;
    private String mode; // Google Meet, In person, Phone, Teams
    private String status; // Scheduled, Completed, Cancelled
}
