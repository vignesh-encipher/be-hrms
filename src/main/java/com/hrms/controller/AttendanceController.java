package com.hrms.controller;

import com.hrms.entity.Attendance;
import com.hrms.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/clock-in")
    public ResponseEntity<Attendance> clockIn(
            @RequestParam String employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(attendanceService.clockIn(employeeId, status, remarks));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<Attendance> clockOut(@RequestParam String employeeId) {
        return ResponseEntity.ok(attendanceService.clockOut(employeeId));
    }

    @GetMapping("/today")
    public ResponseEntity<Attendance> getTodayAttendance(@RequestParam String employeeId) {
        return ResponseEntity.ok(attendanceService.getTodayAttendance(employeeId));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<Attendance>> getMonthlyAttendance(
            @RequestParam String employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(attendanceService.getMonthlyAttendance(employeeId, month, year));
    }

    @GetMapping("/date")
    public ResponseEntity<List<Attendance>> getAllAttendanceForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAllAttendanceForDate(date));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetDailyAttendance() {
        attendanceService.resetDailyAttendance();
        return ResponseEntity.ok().build();
    }
}
