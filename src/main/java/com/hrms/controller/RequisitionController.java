package com.hrms.controller;

import com.hrms.entity.Requisition;
import com.hrms.service.RequisitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/requisitions")
public class RequisitionController {

    @Autowired
    private RequisitionService requisitionService;

    @PostMapping("/raise")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public ResponseEntity<Requisition> raiseRequisition(@RequestBody Requisition requisition) {
        return ResponseEntity.ok(requisitionService.raiseRequisition(requisition));
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public ResponseEntity<Requisition> approveRequisition(
            @PathVariable String id,
            @RequestParam String role,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(requisitionService.approveRequisition(id, role, remarks));
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public ResponseEntity<Requisition> rejectRequisition(
            @PathVariable String id,
            @RequestParam String role,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(requisitionService.rejectRequisition(id, role, remarks));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public ResponseEntity<List<Requisition>> getAllRequisitions() {
        return ResponseEntity.ok(requisitionService.getAllRequisitions());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or hasRole('FINANCE') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public ResponseEntity<List<Requisition>> getPendingRequisitions() {
        return ResponseEntity.ok(requisitionService.getPendingRequisitions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Requisition> getRequisitionById(@PathVariable String id) {
        return ResponseEntity.ok(requisitionService.getRequisitionById(id));
    }

    @GetMapping("/requester/{employeeId}")
    public ResponseEntity<List<Requisition>> getRequisitionsByRequester(@PathVariable String employeeId) {
        return ResponseEntity.ok(requisitionService.getRequisitionsByRequester(employeeId));
    }
}
