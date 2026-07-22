package com.hrms.controller;

import com.hrms.entity.LeaveRequest;
import com.hrms.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/leaves")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping("/apply")
    public ResponseEntity<LeaveRequest> applyLeave(@RequestBody LeaveRequest leaveRequest) {
        return ResponseEntity.ok(leaveService.applyLeave(leaveRequest));
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<LeaveRequest> approveLeave(
            @PathVariable String id,
            @RequestParam String role,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(leaveService.approveLeave(id, role, remarks));
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<LeaveRequest> rejectLeave(
            @PathVariable String id,
            @RequestParam String role,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(leaveService.rejectLeave(id, role, remarks));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequest>> getLeaveHistory(@PathVariable String employeeId) {
        return ResponseEntity.ok(leaveService.getLeaveHistory(employeeId));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<List<LeaveRequest>> getAllLeaveRequests() {
        return ResponseEntity.ok(leaveService.getAllLeaveRequests());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<LeaveRequest>> getPendingLeaveRequests() {
        return ResponseEntity.ok(leaveService.getPendingLeaveRequests());
    }

    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<Map<String, Double>> getLeaveBalance(@PathVariable String employeeId) {
        return ResponseEntity.ok(leaveService.getLeaveBalance(employeeId));
    }
}
