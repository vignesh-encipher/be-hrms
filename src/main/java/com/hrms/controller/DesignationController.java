package com.hrms.controller;

import com.hrms.entity.Designation;
import com.hrms.service.DesignationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/designations")
public class DesignationController {

    @Autowired
    private DesignationService designationService;

    @GetMapping
    public ResponseEntity<List<Designation>> getAllDesignations() {
        return ResponseEntity.ok(designationService.getAllDesignations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Designation> getDesignationById(@PathVariable String id) {
        return ResponseEntity.ok(designationService.getDesignationById(id));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<Designation>> getDesignationsByDepartment(@PathVariable String departmentId) {
        return ResponseEntity.ok(designationService.getDesignationsByDepartment(departmentId));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<Designation> createDesignation(@RequestBody Designation designation) {
        return ResponseEntity.ok(designationService.createDesignation(designation));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<Designation> updateDesignation(@PathVariable String id, @RequestBody Designation designation) {
        return ResponseEntity.ok(designationService.updateDesignation(id, designation));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<Void> deleteDesignation(@PathVariable String id) {
        designationService.deleteDesignation(id);
        return ResponseEntity.noContent().build();
    }
}
