package com.hrms.controller;

import com.hrms.entity.LeaveType;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.LeaveTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/leave")
public class LeaveSettingsController {

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    private void seedDefaultLeaveTypesIfEmpty() {
        if (leaveTypeRepository.count() == 0) {
            List<LeaveType> defaults = new ArrayList<>();
            defaults.add(LeaveType.builder().name("Casual Leave (CL)").code("CL").totalDays(12.0).monthlyAccrual(true).carryForwardAllowed(true).maxCarryForwardDays(5.0).encashmentAllowed(false).maxPerRequest(3.0).validityDays(365).active(true).readOnly(false).build());
            defaults.add(LeaveType.builder().name("Sick Leave (SL)").code("SL").totalDays(8.0).monthlyAccrual(false).carryForwardAllowed(false).maxCarryForwardDays(0.0).encashmentAllowed(false).maxPerRequest(3.0).validityDays(365).active(true).readOnly(false).build());
            defaults.add(LeaveType.builder().name("Earned Leave (EL)").code("EL").totalDays(15.0).monthlyAccrual(true).carryForwardAllowed(true).maxCarryForwardDays(10.0).encashmentAllowed(true).maxPerRequest(5.0).validityDays(365).active(true).readOnly(false).build());
            defaults.add(LeaveType.builder().name("Maternity Leave").code("MAT").totalDays(180.0).monthlyAccrual(false).carryForwardAllowed(false).maxCarryForwardDays(0.0).encashmentAllowed(false).maxPerRequest(180.0).validityDays(365).active(true).readOnly(false).build());
            defaults.add(LeaveType.builder().name("Paternity Leave").code("PAT").totalDays(15.0).monthlyAccrual(false).carryForwardAllowed(false).maxCarryForwardDays(0.0).encashmentAllowed(false).maxPerRequest(15.0).validityDays(365).active(true).readOnly(false).build());
            defaults.add(LeaveType.builder().name("Marriage Leave").code("MAR").totalDays(5.0).monthlyAccrual(false).carryForwardAllowed(false).maxCarryForwardDays(0.0).encashmentAllowed(false).maxPerRequest(5.0).validityDays(365).active(true).readOnly(false).build());
            defaults.add(LeaveType.builder().name("Bereavement Leave").code("BER").totalDays(5.0).monthlyAccrual(false).carryForwardAllowed(false).maxCarryForwardDays(0.0).encashmentAllowed(false).maxPerRequest(5.0).validityDays(365).active(true).readOnly(false).build());
            defaults.add(LeaveType.builder().name("Optional Holiday").code("OPH").totalDays(3.0).monthlyAccrual(false).carryForwardAllowed(false).maxCarryForwardDays(0.0).encashmentAllowed(false).maxPerRequest(1.0).validityDays(365).active(true).readOnly(false).build());
            defaults.add(LeaveType.builder().name("Work From Home (WFH)").code("WFH").totalDays(24.0).monthlyAccrual(true).carryForwardAllowed(false).maxCarryForwardDays(0.0).encashmentAllowed(false).maxPerRequest(5.0).validityDays(365).active(true).readOnly(false).build());
            defaults.add(LeaveType.builder().name("Loss of Pay (LOP)").code("LOP").totalDays(0.0).monthlyAccrual(false).carryForwardAllowed(false).maxCarryForwardDays(0.0).encashmentAllowed(false).maxPerRequest(365.0).validityDays(365).active(true).readOnly(true).build());
            leaveTypeRepository.saveAll(defaults);
        }
    }

    @GetMapping("/types")
    public ResponseEntity<List<LeaveType>> getLeaveTypes() {
        seedDefaultLeaveTypesIfEmpty();
        return ResponseEntity.ok(leaveTypeRepository.findAll());
    }

    @GetMapping("/settings")
    public ResponseEntity<List<LeaveType>> getLeaveSettings() {
        seedDefaultLeaveTypesIfEmpty();
        return ResponseEntity.ok(leaveTypeRepository.findAll());
    }

    @PostMapping("/type")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<LeaveType> addLeaveType(@RequestBody LeaveType leaveType) {
        if (leaveType.getTotalDays() != null && leaveType.getTotalDays() < 0) {
            throw new BadRequestException("Total leave days cannot be negative");
        }
        if (Boolean.TRUE.equals(leaveType.getCarryForwardAllowed()) && 
            leaveType.getMaxCarryForwardDays() != null && 
            leaveType.getTotalDays() != null && 
            leaveType.getMaxCarryForwardDays() > leaveType.getTotalDays()) {
            throw new BadRequestException("Max carry forward days cannot exceed total leave days");
        }
        return ResponseEntity.ok(leaveTypeRepository.save(leaveType));
    }

    @PutMapping("/type/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<LeaveType> updateLeaveType(@PathVariable String id, @RequestBody LeaveType payload) {
        LeaveType existing = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + id));

        if (payload.getTotalDays() != null && payload.getTotalDays() < 0) {
            throw new BadRequestException("Total leave days cannot be negative");
        }
        if (Boolean.TRUE.equals(payload.getCarryForwardAllowed()) && 
            payload.getMaxCarryForwardDays() != null && 
            payload.getTotalDays() != null && 
            payload.getMaxCarryForwardDays() > payload.getTotalDays()) {
            throw new BadRequestException("Max carry forward days cannot exceed total leave days");
        }

        existing.setName(payload.getName());
        existing.setCode(payload.getCode());
        existing.setTotalDays(payload.getTotalDays());
        existing.setMonthlyAccrual(payload.getMonthlyAccrual());
        existing.setCarryForwardAllowed(payload.getCarryForwardAllowed());
        existing.setMaxCarryForwardDays(payload.getMaxCarryForwardDays());
        existing.setEncashmentAllowed(payload.getEncashmentAllowed());
        existing.setMaxPerRequest(payload.getMaxPerRequest());
        existing.setValidityDays(payload.getValidityDays());
        existing.setActive(payload.getActive());

        return ResponseEntity.ok(leaveTypeRepository.save(existing));
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<LeaveType>> updateLeaveSettings(@RequestBody List<LeaveType> settingsList) {
        for (LeaveType lt : settingsList) {
            if (lt.getTotalDays() != null && lt.getTotalDays() < 0) {
                throw new BadRequestException("Leave days cannot be negative for " + lt.getName());
            }
            if (Boolean.TRUE.equals(lt.getCarryForwardAllowed()) && 
                lt.getMaxCarryForwardDays() != null && 
                lt.getTotalDays() != null && 
                lt.getMaxCarryForwardDays() > lt.getTotalDays()) {
                throw new BadRequestException("Max carry forward days cannot exceed total leave days for " + lt.getName());
            }
        }
        List<LeaveType> saved = leaveTypeRepository.saveAll(settingsList);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/type/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteLeaveType(@PathVariable String id) {
        LeaveType existing = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + id));
        if (Boolean.TRUE.equals(existing.getReadOnly())) {
            throw new BadRequestException("System read-only leave type cannot be deleted");
        }
        leaveTypeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
