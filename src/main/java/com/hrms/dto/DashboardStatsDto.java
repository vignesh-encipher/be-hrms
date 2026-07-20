package com.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalEmployees;
    private long presentToday;
    private long absentToday;
    private long pendingLeaves;
    private long departmentsCount;
    private List<Map<String, Object>> upcomingBirthdays;
    
    // Chart stats
    private List<Map<String, Object>> attendanceTrend;
    private List<Map<String, Object>> leaveStatistics; // e.g. [ { "type": "Sick Leave", "count": 5 }, ... ]
    private List<Map<String, Object>> departmentWiseEmployees; // [ { "department": "IT", "count": 25 }, ... ]
    
    private List<Map<String, Object>> recentActivities;
}
