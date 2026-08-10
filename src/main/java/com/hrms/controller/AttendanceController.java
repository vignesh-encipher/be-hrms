package com.hrms.controller;

import com.hrms.dto.AttendanceStatusDto;
import com.hrms.dto.MonthlyAttendanceSummaryDto;
import com.hrms.dto.MonthlyRegisterRowDto;
import com.hrms.dto.TeamAttendanceDto;
import com.hrms.entity.Attendance;
import com.hrms.entity.Employee;
import com.hrms.entity.Shift;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.EmployeeRepository;
import com.hrms.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private EmployeeRepository employeeRepository;

    // Resolve the authenticated employee server-side, the same way LeaveServiceImpl does,
    // so a client can never check-in/out/break as someone else.
    private String currentEmployeeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        Employee employee = employeeRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated employee not found"));
        return employee.getEmployeeId();
    }

    private String ip(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String ua(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    // ---------- check-in / check-out / breaks (self, resolved from auth) ----------

    @PostMapping({"/check-in", "/clock-in"})
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<Attendance> checkIn(HttpServletRequest request) {
        return ResponseEntity.ok(attendanceService.checkIn(currentEmployeeId(), ip(request), ua(request)));
    }

    @PostMapping({"/check-out", "/clock-out"})
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<Attendance> checkOut(HttpServletRequest request) {
        return ResponseEntity.ok(attendanceService.checkOut(currentEmployeeId(), ip(request), ua(request)));
    }

    @PostMapping("/break/start")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<Attendance> startBreak(HttpServletRequest request) {
        return ResponseEntity.ok(attendanceService.startBreak(currentEmployeeId(), ip(request), ua(request)));
    }

    @PostMapping("/break/end")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<Attendance> endBreak(HttpServletRequest request) {
        return ResponseEntity.ok(attendanceService.endBreak(currentEmployeeId(), ip(request), ua(request)));
    }

    // ---------- self reads ----------

    @GetMapping("/today")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<AttendanceStatusDto> getTodayAttendance(@RequestParam(required = false) String employeeId) {
        String id = employeeId != null ? employeeId : currentEmployeeId();
        return ResponseEntity.ok(attendanceService.getTodayStatus(id));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<Attendance>> getHistory(
            @RequestParam(required = false) String employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String id = employeeId != null ? employeeId : currentEmployeeId();
        return ResponseEntity.ok(attendanceService.getHistory(id, from, to));
    }

    @GetMapping("/{date}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<Attendance> getAttendanceForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String employeeId) {
        String id = employeeId != null ? employeeId : currentEmployeeId();
        return ResponseEntity.ok(attendanceService.getAttendanceForDate(id, date));
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<Attendance>> getMonthlyAttendance(
            @RequestParam(required = false) String employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        String id = employeeId != null ? employeeId : currentEmployeeId();
        return ResponseEntity.ok(attendanceService.getMonthlyAttendance(id, month, year));
    }

    @GetMapping("/monthly-summary")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<MonthlyAttendanceSummaryDto> getMonthlySummary(
            @RequestParam(required = false) String employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        String id = employeeId != null ? employeeId : currentEmployeeId();
        return ResponseEntity.ok(attendanceService.getMonthlySummary(id, month, year));
    }

    // ---------- manager / HR views ----------

    @GetMapping("/team")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<List<TeamAttendanceDto>> getTeamAttendance(
            @RequestParam(required = false) String managerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String id = managerId != null ? managerId : currentEmployeeId();
        LocalDate d = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(attendanceService.getTeamAttendance(id, d));
    }

    @GetMapping("/date")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<List<Attendance>> getAllAttendanceForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAllAttendanceForDate(date));
    }

    // HR/Admin biometric-register-style monthly grid: every active employee x every
    // day of the month, with a single-letter status (P/A/L/H/weekly-off). Read-only.
    @GetMapping("/monthly-register")
    @PreAuthorize("hasRole('HR') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<MonthlyRegisterRowDto>> getMonthlyRegister(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(required = false) String departmentId) {
        return ResponseEntity.ok(attendanceService.getMonthlyRegister(month, year, departmentId));
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<Void> resetDailyAttendance() {
        attendanceService.resetDailyAttendance();
        return ResponseEntity.ok().build();
    }

    // ---------- shift / rules ----------

    @GetMapping("/rules")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<Shift>> getRules() {
        return ResponseEntity.ok(attendanceService.getShifts());
    }

    @GetMapping("/shifts")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<List<Shift>> getShifts() {
        return ResponseEntity.ok(attendanceService.getShifts());
    }

    @PostMapping("/shifts")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<Shift> createShift(@RequestBody Shift shift) {
        return ResponseEntity.ok(attendanceService.createShift(shift));
    }

    @PutMapping("/shifts/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<Shift> updateShift(@PathVariable String id, @RequestBody Shift shift) {
        return ResponseEntity.ok(attendanceService.updateShift(id, shift));
    }
}
