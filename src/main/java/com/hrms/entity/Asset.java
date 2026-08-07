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
@Document(collection = "assets")
public class Asset {
    @Id
    private String id;

    private String assetTag; // unique

    private String kind; // Laptop, Desktop, Monitor, Keyboard, Mouse, ID Card, Phone
    private String model;

    private String assignedToEmployeeId; // nullable
    private LocalDate assignedSince; // nullable
    private LocalDate warrantyTill; // nullable
    private String amcVendor; // nullable

    private String status; // In stock, Assigned, Recovery due, Retired

    @Builder.Default
    private List<AssetHistoryEntry> history = new ArrayList<>();
}
