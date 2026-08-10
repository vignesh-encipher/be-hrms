package com.hrms.service;

import com.hrms.dto.AttendanceStatusDto;
import com.hrms.dto.MonthlyAttendanceSummaryDto;
import com.hrms.dto.MonthlyRegisterRowDto;
import com.hrms.dto.TeamAttendanceDto;
import com.hrms.entity.Attendance;
import com.hrms.entity.Shift;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {
    Attendance checkIn(String employeeId, String ipAddress, String userAgent);
    Attendance checkOut(String employeeId, String ipAddress, String userAgent);
    Attendance startBreak(String employeeId, String ipAddress, String userAgent);
    Attendance endBreak(String employeeId, String ipAddress, String userAgent);

    AttendanceStatusDto getTodayStatus(String employeeId);
    Attendance getTodayAttendance(String employeeId);

    List<Attendance> getHistory(String employeeId, LocalDate from, LocalDate to);
    Attendance getAttendanceForDate(String employeeId, LocalDate date);

    List<Attendance> getMonthlyAttendance(String employeeId, int month, int year);
    MonthlyAttendanceSummaryDto getMonthlySummary(String employeeId, int month, int year);

    List<TeamAttendanceDto> getTeamAttendance(String managerId, LocalDate date);

    List<Attendance> getAllAttendanceForDate(LocalDate date);

    // HR/Admin monthly register grid (biometric-register-style view). Read-only.
    List<MonthlyRegisterRowDto> getMonthlyRegister(int month, int year, String departmentId);

    void resetDailyAttendance();

    // Shift management
    Shift createShift(Shift shift);
    List<Shift> getShifts();
    Shift updateShift(String id, Shift shift);

    // Shared helpers used by RegularizationServiceImpl when a correction is approved.
    Shift resolveShift(String employeeId);
    void recomputeAggregates(Attendance attendance, Shift shift);
}
