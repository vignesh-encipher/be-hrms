package com.hrms.service;

import com.hrms.entity.Attendance;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {
    Attendance clockIn(String employeeId, String status, String remarks);
    Attendance clockOut(String employeeId);
    Attendance getTodayAttendance(String employeeId);
    List<Attendance> getMonthlyAttendance(String employeeId, int month, int year);
    List<Attendance> getAllAttendanceForDate(LocalDate date);
    void resetDailyAttendance();
}
