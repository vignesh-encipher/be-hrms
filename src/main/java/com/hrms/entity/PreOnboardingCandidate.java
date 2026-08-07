package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pre_onboarding_candidates")
public class PreOnboardingCandidate {

    @Id
    private String id;

    private String name;
    private String email;
    private String phone;
    private String designation; // role
    private LocalDate dateOfJoining;
    private String reportingManagerId; // employeeId of manager
    private String workLocation;
    private String shift;

    private String bgvStatus; // Not Started, In Progress, Cleared

    @Builder.Default
    private List<ChecklistItem> documentsChecklist = new ArrayList<>();

    @Builder.Default
    private List<ChecklistItem> itAdminChecklist = new ArrayList<>();

    @Builder.Default
    private List<ChecklistItem> hrChecklist = new ArrayList<>();

    private String status; // In Progress, Converted, Archived

    private String convertedEmployeeId;

    // Computed, not persisted: 0-4 (Offer accepted / Documents / BGV / IT setup / Ready to join)
    @org.springframework.data.annotation.Transient
    private Integer stage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChecklistItem {
        private String item;
        private boolean done;
    }
}
