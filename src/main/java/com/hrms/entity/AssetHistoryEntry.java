package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetHistoryEntry {
    private String employeeId;
    private LocalDate assignedOn;
    private LocalDate returnedOn; // null while still assigned
    private String conditionNotes;
}
