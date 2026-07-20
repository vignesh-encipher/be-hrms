package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "designations")
public class Designation {
    @Id
    private String id;

    @Indexed(unique = true)
    private String title;

    private String departmentId; // Reference to Department
    private String description;
}
