package com.hrms.controller;

import com.hrms.entity.BankAdviceRow;
import com.hrms.entity.PayrollRun;
import com.hrms.service.BankAdviceService;
import com.hrms.service.PayrollRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/payroll")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE')")
public class PayrollRunController {

    @Autowired
    private PayrollRunService payrollRunService;

    @Autowired
    private BankAdviceService bankAdviceService;

    @PostMapping("/runs")
    public ResponseEntity<PayrollRun> createRun(@RequestBody PayrollRun run) {
        return ResponseEntity.ok(payrollRunService.createRun(run));
    }

    @GetMapping("/runs")
    public ResponseEntity<List<PayrollRun>> getAllRuns() {
        return ResponseEntity.ok(payrollRunService.getAllRuns());
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<PayrollRun> getRunById(@PathVariable String id) {
        return ResponseEntity.ok(payrollRunService.getRunById(id));
    }

    @PostMapping("/runs/advanceStage/{id}")
    public ResponseEntity<PayrollRun> advanceStage(@PathVariable String id) {
        return ResponseEntity.ok(payrollRunService.advanceStage(id));
    }

    @GetMapping("/bank-advice/{period}")
    public ResponseEntity<List<BankAdviceRow>> getBankAdvice(@PathVariable String period) {
        return ResponseEntity.ok(bankAdviceService.getBankAdvice(period));
    }
}
