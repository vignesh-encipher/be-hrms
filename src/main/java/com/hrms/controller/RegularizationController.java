package com.hrms.controller;

import com.hrms.entity.RegularizationRequest;
import com.hrms.service.RegularizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/regularizations")
public class RegularizationController {

    @Autowired
    private RegularizationService regularizationService;

    @PostMapping("/apply")
    public ResponseEntity<RegularizationRequest> apply(@RequestBody RegularizationRequest request) {
        return ResponseEntity.ok(regularizationService.apply(request));
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<RegularizationRequest> approve(
            @PathVariable String id,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(regularizationService.approve(id, remarks));
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<RegularizationRequest> reject(
            @PathVariable String id,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(regularizationService.reject(id, remarks));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<RegularizationRequest>> getByEmployee(@PathVariable String employeeId) {
        return ResponseEntity.ok(regularizationService.getByEmployee(employeeId));
    }

    @GetMapping("/manager/{managerId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<RegularizationRequest>> getPendingByManager(@PathVariable String managerId) {
        return ResponseEntity.ok(regularizationService.getPendingByManager(managerId));
    }
}
