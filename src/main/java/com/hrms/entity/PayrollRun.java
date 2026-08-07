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
@Document(collection = "payroll_runs")
public class PayrollRun {
    @Id
    private String id;

    private String period; // e.g. "2026-08"
    private String status; // Draft / Processing / Approved / Paid
    private int stage; // 0..6 : Input lock, Computed, Finance review, HR approved, Bank advice, Paid
    private LocalDate processedDate;
    private String processedBy;
}
