package com.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * One row of the HR "Monthly Register" grid (biometric-register-style view):
 * an employee's day-by-day status letter for every day of the requested month.
 *
 * days key = day-of-month as a string ("1".."31"), value = single-letter status:
 *   P = Present, A = Absent/missing punch, L = Leave, H = Holiday, "·" = Weekly off,
 *   "" = future date within the month (not yet applicable).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRegisterRowDto {
    private String employeeId;
    private String employeeCode;
    private String name;
    private Map<String, String> days;
    private long presentCount;
}
