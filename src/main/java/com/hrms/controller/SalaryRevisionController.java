package com.hrms.controller;

import com.hrms.entity.SalaryRevision;
import com.hrms.service.SalaryRevisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/payroll/revisions")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE')")
public class SalaryRevisionController {

    @Autowired
    private SalaryRevisionService salaryRevisionService;

    @PostMapping
    public ResponseEntity<SalaryRevision> createRevision(@RequestBody SalaryRevision revision) {
        return ResponseEntity.ok(salaryRevisionService.createRevision(revision));
    }

    @GetMapping
    public ResponseEntity<List<SalaryRevision>> getAllRevisions() {
        return ResponseEntity.ok(salaryRevisionService.getAllRevisions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaryRevision> getRevisionById(@PathVariable String id) {
        return ResponseEntity.ok(salaryRevisionService.getRevisionById(id));
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<SalaryRevision> approveRevision(@PathVariable String id) {
        return ResponseEntity.ok(salaryRevisionService.approveRevision(id));
    }
}
