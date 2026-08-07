package com.hrms.controller;

import com.hrms.entity.ProbationRecord;
import com.hrms.service.ProbationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/probation")
public class ProbationController {

    @Autowired
    private ProbationService probationService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<ProbationRecord> createProbationRecord(@RequestBody ProbationRecord record) {
        return ResponseEntity.ok(probationService.createProbationRecord(record));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<List<ProbationRecord>> getAll(@RequestParam(required = false) String filter) {
        return ResponseEntity.ok(probationService.getAll(filter));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<ProbationRecord> getById(@PathVariable String id) {
        return ResponseEntity.ok(probationService.getById(id));
    }

    @PostMapping("/{id}/evaluate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<ProbationRecord> evaluate(@PathVariable String id, @RequestBody List<ProbationRecord.RatingItem> ratings) {
        return ResponseEntity.ok(probationService.submitEvaluation(id, ratings));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER')")
    public ResponseEntity<ProbationRecord> decision(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String decisionType = (String) body.get("decisionType");
        String remarks = (String) body.get("remarks");
        Integer extendByMonths = body.get("extendByMonths") != null
                ? Integer.valueOf(body.get("extendByMonths").toString())
                : null;
        return ResponseEntity.ok(probationService.submitDecision(id, decisionType, extendByMonths, remarks));
    }
}
