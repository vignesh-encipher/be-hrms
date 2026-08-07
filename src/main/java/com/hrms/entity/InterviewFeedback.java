package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewFeedback {
    private String round;
    private String interviewerName;
    private Integer score; // 1-5
    private String comments;
    private LocalDateTime submittedAt;
}
