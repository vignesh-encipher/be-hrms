package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "job_openings")
public class JobOpening {
    @Id
    private String id;

    private String title;
    private String department;
    private String designation;
    private Integer positions;
    private String status; // Open, Closed
    private String requisitionId; // optional, sourced-from requisition
    private String description;
    private String location;
}
