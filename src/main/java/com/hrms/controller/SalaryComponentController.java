package com.hrms.controller;

import com.hrms.entity.SalaryComponent;
import com.hrms.service.SalaryComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/payroll/components")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE')")
public class SalaryComponentController {

    @Autowired
    private SalaryComponentService salaryComponentService;

    @PostMapping
    public ResponseEntity<SalaryComponent> createComponent(@RequestBody SalaryComponent component) {
        return ResponseEntity.ok(salaryComponentService.createComponent(component));
    }

    @GetMapping
    public ResponseEntity<List<SalaryComponent>> getAllComponents() {
        return ResponseEntity.ok(salaryComponentService.getAllComponents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaryComponent> getComponentById(@PathVariable String id) {
        return ResponseEntity.ok(salaryComponentService.getComponentById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalaryComponent> updateComponent(@PathVariable String id, @RequestBody SalaryComponent component) {
        return ResponseEntity.ok(salaryComponentService.updateComponent(id, component));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComponent(@PathVariable String id) {
        salaryComponentService.deleteComponent(id);
        return ResponseEntity.noContent().build();
    }
}
