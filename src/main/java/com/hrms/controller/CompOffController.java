package com.hrms.controller;

import com.hrms.entity.CompOffRequest;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.CompOffRepository;
import com.hrms.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/compoff")
public class CompOffController {

    @Autowired
    private CompOffRepository compOffRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getCompOffSummary(@RequestParam(required = false) String employeeId) {
        List<CompOffRequest> requests = (employeeId != null && !employeeId.isEmpty()) 
                ? compOffRepository.findByEmployeeId(employeeId)
                : compOffRepository.findAll();

        double available = 0;
        double used = 0;
        double pending = 0;
        double expired = 0;

        for (CompOffRequest r : requests) {
            String status = r.getStatus() != null ? r.getStatus() : "Pending";
            double earned = r.getEarnedDays() != null ? r.getEarnedDays() : 1.0;
            switch (status) {
                case "Approved":
                    available += earned;
                    break;
                case "Used":
                    used += earned;
                    break;
                case "Pending":
                    pending += 1.0;
                    break;
                case "Expired":
                    expired += earned;
                    break;
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("available", available);
        summary.put("used", used);
        summary.put("pending", pending);
        summary.put("expired", expired);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/history")
    public ResponseEntity<List<CompOffRequest>> getCompOffHistory(@RequestParam(required = false) String employeeId) {
        List<CompOffRequest> list;
        if (employeeId != null && !employeeId.isEmpty()) {
            list = compOffRepository.findByEmployeeId(employeeId);
        } else {
            list = compOffRepository.findAll();
        }
        
        // Sort descending by workedDate (mutable array list wrapping)
        List<CompOffRequest> mutableList = new java.util.ArrayList<>(list);
        mutableList.sort((a, b) -> {
            if (b.getWorkedDate() == null && a.getWorkedDate() == null) return 0;
            if (b.getWorkedDate() == null) return -1;
            if (a.getWorkedDate() == null) return 1;
            return b.getWorkedDate().compareTo(a.getWorkedDate());
        });
        
        return ResponseEntity.ok(mutableList);
    }

    @PostMapping("/request")
    public ResponseEntity<CompOffRequest> requestCompOff(@RequestBody CompOffRequest request) {
        if (request.getWorkedDate() == null) {
            throw new BadRequestException("Worked date is required");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BadRequestException("Reason is required");
        }

        // Calculate hours if start & end time present
        if (request.getStartTime() != null && request.getEndTime() != null) {
            try {
                LocalTime start = LocalTime.parse(request.getStartTime());
                LocalTime end = LocalTime.parse(request.getEndTime());
                double hours = Duration.between(start, end).toMinutes() / 60.0;
                if (hours < 4.0) {
                    throw new BadRequestException("Minimum working hours for Comp Off is 4 hours");
                }
                request.setHoursWorked(hours);
                request.setEarnedDays(hours >= 8.0 ? 1.0 : 0.5);
            } catch (Exception e) {
                if (e instanceof BadRequestException) throw e;
            }
        }

        if (request.getHoursWorked() == null) {
            request.setHoursWorked(8.0);
            request.setEarnedDays(1.0);
        }

        request.setRequestDate(LocalDate.now());
        request.setStatus("Pending");
        request.setExpiryDate(request.getWorkedDate().plusDays(90));

        return ResponseEntity.ok(compOffRepository.save(request));
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<CompOffRequest> approveCompOff(
            @PathVariable String id, 
            @RequestParam(required = false, defaultValue = "Admin") String approvedBy,
            @RequestParam(required = false) String remarks) {
        CompOffRequest existing = compOffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comp Off request not found: " + id));

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : "";
        com.hrms.entity.Employee currentApprover = employeeRepository.findByEmail(currentUsername)
                .or(() -> employeeRepository.findByEmployeeId(currentUsername))
                .orElse(null);

        com.hrms.entity.Employee requester = employeeRepository.findByEmployeeId(existing.getEmployeeId())
                .or(() -> employeeRepository.findByEmail(existing.getEmployeeId()))
                .orElse(null);

        if (requester != null && currentApprover != null) {
            String managerId = requester.getManagerId();
            boolean isManager = managerId != null && (
                managerId.equals(currentApprover.getEmployeeId()) ||
                managerId.equals(currentApprover.getEmail())
            );

            boolean isHR = currentApprover.getRoles().contains(com.hrms.entity.ERole.ROLE_HR) ||
                           currentApprover.getRoles().contains(com.hrms.entity.ERole.ROLE_SUPER_ADMIN);

            if (!isHR && !isManager) {
                throw new BadRequestException("Only the direct reporting manager or HR can approve this Comp-Off request!");
            }
        }

        existing.setStatus("Approved");
        existing.setApprovedBy(approvedBy);
        existing.setRemarks(remarks != null ? remarks : "Approved");

        return ResponseEntity.ok(compOffRepository.save(existing));
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<CompOffRequest> rejectCompOff(
            @PathVariable String id, 
            @RequestParam(required = false, defaultValue = "Admin") String approvedBy,
            @RequestParam(required = false) String remarks) {
        CompOffRequest existing = compOffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comp Off request not found: " + id));

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : "";
        com.hrms.entity.Employee currentApprover = employeeRepository.findByEmail(currentUsername)
                .or(() -> employeeRepository.findByEmployeeId(currentUsername))
                .orElse(null);

        com.hrms.entity.Employee requester = employeeRepository.findByEmployeeId(existing.getEmployeeId())
                .or(() -> employeeRepository.findByEmail(existing.getEmployeeId()))
                .orElse(null);

        if (requester != null && currentApprover != null) {
            String managerId = requester.getManagerId();
            boolean isManager = managerId != null && (
                managerId.equals(currentApprover.getEmployeeId()) ||
                managerId.equals(currentApprover.getEmail())
            );

            boolean isHR = currentApprover.getRoles().contains(com.hrms.entity.ERole.ROLE_HR) ||
                           currentApprover.getRoles().contains(com.hrms.entity.ERole.ROLE_SUPER_ADMIN);

            if (!isHR && !isManager) {
                throw new BadRequestException("Only the direct reporting manager or HR can reject this Comp-Off request!");
            }
        }

        existing.setStatus("Rejected");
        existing.setApprovedBy(approvedBy);
        existing.setRemarks(remarks != null ? remarks : "Rejected");

        return ResponseEntity.ok(compOffRepository.save(existing));
    }
}
