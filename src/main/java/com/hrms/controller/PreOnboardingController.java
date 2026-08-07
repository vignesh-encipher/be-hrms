package com.hrms.controller;

import com.hrms.dto.EmployeeDto;
import com.hrms.entity.PreOnboardingCandidate;
import com.hrms.service.PreOnboardingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/pre-onboarding")
public class PreOnboardingController {

    @Autowired
    private PreOnboardingService preOnboardingService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('RECRUITER') or hasRole('IT_ADMIN')")
    public ResponseEntity<PreOnboardingCandidate> createCandidate(@RequestBody PreOnboardingCandidate candidate) {
        return ResponseEntity.ok(preOnboardingService.createCandidate(candidate));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('RECRUITER') or hasRole('IT_ADMIN')")
    public ResponseEntity<List<PreOnboardingCandidate>> getAllCandidates() {
        return ResponseEntity.ok(preOnboardingService.getAllCandidates());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('RECRUITER') or hasRole('IT_ADMIN')")
    public ResponseEntity<PreOnboardingCandidate> getCandidateById(@PathVariable String id) {
        return ResponseEntity.ok(preOnboardingService.getCandidateById(id));
    }

    @PostMapping("/{id}/checklist/toggle")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('RECRUITER') or hasRole('IT_ADMIN')")
    public ResponseEntity<PreOnboardingCandidate> toggleChecklistItem(
            @PathVariable String id,
            @RequestParam String checklistType,
            @RequestParam String item,
            @RequestParam boolean done) {
        return ResponseEntity.ok(preOnboardingService.toggleChecklistItem(id, checklistType, item, done));
    }

    @PostMapping("/{id}/bgv")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('RECRUITER') or hasRole('IT_ADMIN')")
    public ResponseEntity<PreOnboardingCandidate> updateBgvStatus(
            @PathVariable String id,
            @RequestParam String bgvStatus) {
        return ResponseEntity.ok(preOnboardingService.updateBgvStatus(id, bgvStatus));
    }

    @PostMapping("/{id}/convert-to-employee")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<EmployeeDto> convertToEmployee(@PathVariable String id, @RequestBody(required = false) EmployeeDto employeeDto) {
        EmployeeDto payload = employeeDto != null ? employeeDto : EmployeeDto.builder().build();
        return ResponseEntity.ok(preOnboardingService.convertToEmployee(id, payload));
    }
}
