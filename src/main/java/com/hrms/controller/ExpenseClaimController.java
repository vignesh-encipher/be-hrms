package com.hrms.controller;

import com.hrms.entity.ExpenseClaim;
import com.hrms.service.ExpenseClaimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/reimbursements")
public class ExpenseClaimController {

    @Autowired
    private ExpenseClaimService expenseClaimService;

    @PostMapping(value = "/submit", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('FINANCE') or hasRole('EMPLOYEE')")
    public ResponseEntity<ExpenseClaim> submitClaim(
            @RequestParam String employeeId,
            @RequestParam String category,
            @RequestParam Double amount,
            @RequestParam(required = false) String expenseDate,
            @RequestParam(required = false) String costCentre,
            @RequestParam(required = false) String description,
            @RequestParam(value = "receipts", required = false) MultipartFile[] receipts) {

        ExpenseClaim claim = ExpenseClaim.builder()
                .employeeId(employeeId)
                .category(category)
                .amount(amount)
                .expenseDate(expenseDate != null && !expenseDate.isEmpty() ? LocalDate.parse(expenseDate) : null)
                .costCentre(costCentre)
                .description(description)
                .build();

        return ResponseEntity.ok(expenseClaimService.submitClaim(claim, receipts));
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('FINANCE')")
    public ResponseEntity<ExpenseClaim> approveClaim(
            @PathVariable String id,
            @RequestParam String role,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(expenseClaimService.approve(id, role, remarks));
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('FINANCE')")
    public ResponseEntity<ExpenseClaim> rejectClaim(
            @PathVariable String id,
            @RequestParam String role,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(expenseClaimService.reject(id, role, remarks));
    }

    @PostMapping("/mark-paid/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('FINANCE')")
    public ResponseEntity<ExpenseClaim> markPaid(
            @PathVariable String id,
            @RequestParam String role) {
        return ResponseEntity.ok(expenseClaimService.markPaid(id, role));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<ExpenseClaim>> getClaimHistory(@PathVariable String employeeId) {
        return ResponseEntity.ok(expenseClaimService.getClaimHistory(employeeId));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('FINANCE') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<ExpenseClaim>> getAllClaims() {
        return ResponseEntity.ok(expenseClaimService.getClaimsForRole());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('MANAGER') or hasRole('FINANCE') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<ExpenseClaim>> getPendingClaims() {
        return ResponseEntity.ok(expenseClaimService.getPendingClaimsForRole());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseClaim> getById(@PathVariable String id) {
        return ResponseEntity.ok(expenseClaimService.getById(id));
    }
}
