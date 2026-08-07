package com.hrms.controller;

import com.hrms.entity.SeparationRequest;
import com.hrms.service.SeparationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/separations")
public class SeparationController {

    @Autowired
    private SeparationService separationService;

    @PostMapping("/resign")
    public ResponseEntity<SeparationRequest> submitResignation(@RequestBody SeparationRequest request) {
        return ResponseEntity.ok(separationService.submitResignation(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<SeparationRequest> getById(@PathVariable String id) {
        return ResponseEntity.ok(separationService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE') or hasRole('MANAGER') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<SeparationRequest>> getAll() {
        return ResponseEntity.ok(separationService.getAll());
    }

    @PostMapping("/{id}/clearance")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE') or hasRole('MANAGER')")
    public ResponseEntity<SeparationRequest> toggleClearance(
            @PathVariable String id,
            @RequestParam String department,
            @RequestParam boolean done,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(separationService.toggleClearance(id, department, done, notes));
    }

    @PostMapping("/{id}/asset-return")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE') or hasRole('MANAGER')")
    public ResponseEntity<SeparationRequest> toggleAssetReturn(
            @PathVariable String id,
            @RequestParam String assetTag,
            @RequestParam boolean returned) {
        return ResponseEntity.ok(separationService.toggleAssetReturn(id, assetTag, returned));
    }

    @PostMapping("/{id}/full-and-final")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE')")
    public ResponseEntity<SeparationRequest> generateFullAndFinal(
            @PathVariable String id,
            @RequestBody SeparationRequest.FullAndFinal fullAndFinal) {
        return ResponseEntity.ok(separationService.generateFullAndFinal(id, fullAndFinal));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE') or hasRole('MANAGER')")
    public ResponseEntity<SeparationRequest> approve(
            @PathVariable String id,
            @RequestParam String role,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(separationService.approve(id, role, remarks));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE') or hasRole('MANAGER')")
    public ResponseEntity<SeparationRequest> reject(
            @PathVariable String id,
            @RequestParam String role,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(separationService.reject(id, role, remarks));
    }
}
