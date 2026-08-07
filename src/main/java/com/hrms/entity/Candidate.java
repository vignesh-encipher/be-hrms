package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "candidates")
public class Candidate {
    @Id
    private String id;

    private String name;
    private String email;
    private String phone;
    private String jobId; // applied for job opening id
    private Double experience; // years
    private String source; // Naukri, LinkedIn, Referral, Career page, Consultant
    private Double currentCtc;
    private Double expectedCtc;
    private String noticePeriod;
    private String resumeUrl;

    private String stage; // Applied, Screening, Interview, Offer, Joined, Rejected
    private String rejectionReason;

    @Builder.Default
    private List<InterviewFeedback> feedbackList = new ArrayList<>();
}
