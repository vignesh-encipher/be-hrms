package com.hrms.controller;

import com.hrms.entity.Payroll;
import com.hrms.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/payroll")
public class PayrollController {

    @Autowired
    private PayrollService payrollService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<Payroll> generateSalary(@RequestBody Payroll payroll) {
        return ResponseEntity.ok(payrollService.generateSalary(payroll));
    }

    @GetMapping("/history/{employeeId}")
    public ResponseEntity<List<Payroll>> getPayrollHistory(@PathVariable String employeeId) {
        return ResponseEntity.ok(payrollService.getPayrollHistory(employeeId));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<List<Payroll>> getAllPayrolls() {
        return ResponseEntity.ok(payrollService.getAllPayrolls());
    }

    @GetMapping("/payslip/{id}")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable String id) {
        byte[] pdfBytes = payrollService.generatePayslipPdf(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "payslip-" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
