package com.hrms.controller;

import com.hrms.entity.LoanAdvance;
import com.hrms.service.LoanAdvanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/payroll/loans")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE')")
public class LoanAdvanceController {

    @Autowired
    private LoanAdvanceService loanAdvanceService;

    @PostMapping
    public ResponseEntity<LoanAdvance> createLoan(@RequestBody LoanAdvance loan) {
        return ResponseEntity.ok(loanAdvanceService.createLoan(loan));
    }

    @GetMapping
    public ResponseEntity<List<LoanAdvance>> getAllLoans() {
        return ResponseEntity.ok(loanAdvanceService.getAllLoans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanAdvance> getLoanById(@PathVariable String id) {
        return ResponseEntity.ok(loanAdvanceService.getLoanById(id));
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<LoanAdvance> approveLoan(@PathVariable String id) {
        return ResponseEntity.ok(loanAdvanceService.approveLoan(id));
    }

    @PostMapping("/recordInstalment/{id}")
    public ResponseEntity<LoanAdvance> recordInstalment(@PathVariable String id) {
        return ResponseEntity.ok(loanAdvanceService.recordInstalment(id));
    }
}
