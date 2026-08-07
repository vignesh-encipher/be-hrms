package com.hrms.serviceImpl;

import com.hrms.entity.ApprovalAuditLog;
import com.hrms.entity.Employee;
import com.hrms.entity.SeparationRequest;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.SeparationRepository;
import com.hrms.service.SeparationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SeparationServiceImpl implements SeparationService {

    @Autowired
    private SeparationRepository separationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private static final List<String> DEFAULT_CLEARANCE_DEPARTMENTS =
            List.of("HR", "IT", "Admin", "Finance");

    @Override
    public SeparationRequest submitResignation(SeparationRequest request) {
        Employee employee = employeeRepository.findByEmployeeId(request.getEmployeeId())
                .or(() -> employeeRepository.findById(request.getEmployeeId()))
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.getEmployeeId()));

        if (request.getEmployeeName() == null || request.getEmployeeName().isEmpty()) {
            request.setEmployeeName((employee.getFirstName() != null ? employee.getFirstName() : "") +
                    " " + (employee.getLastName() != null ? employee.getLastName() : ""));
        }
        if (request.getDepartment() == null) {
            request.setDepartment(employee.getDepartmentId());
        }

        if (request.getClearances() == null || request.getClearances().isEmpty()) {
            List<SeparationRequest.Clearance> clearances = new ArrayList<>();
            for (String dept : DEFAULT_CLEARANCE_DEPARTMENTS) {
                clearances.add(SeparationRequest.Clearance.builder()
                        .department(dept)
                        .done(false)
                        .notes("")
                        .build());
            }
            request.setClearances(clearances);
        }

        if (request.getAssetReturns() == null) {
            request.setAssetReturns(new ArrayList<>());
        }

        request.setStatus("Manager Review");
        request.setAuditLogs(new ArrayList<>());

        return separationRepository.save(request);
    }

    @Override
    public SeparationRequest getById(String id) {
        return separationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Separation request not found with id: " + id));
    }

    @Override
    public List<SeparationRequest> getAll() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        List<SeparationRequest> all = separationRepository.findAll();

        if (authentication == null) {
            return all;
        }
        String username = authentication.getName();
        Employee currentEmployee = employeeRepository.findByEmail(username)
                .or(() -> employeeRepository.findByEmployeeId(username))
                .orElse(null);

        if (currentEmployee == null) {
            return all;
        }

        boolean isHR = currentEmployee.getRoles() != null && currentEmployee.getRoles().contains(com.hrms.entity.ERole.ROLE_HR);
        boolean isFinance = currentEmployee.getRoles() != null && currentEmployee.getRoles().contains(com.hrms.entity.ERole.ROLE_FINANCE);
        boolean isSuperAdmin = currentEmployee.getRoles() != null && currentEmployee.getRoles().contains(com.hrms.entity.ERole.ROLE_SUPER_ADMIN);

        if (isHR || isFinance || isSuperAdmin) {
            return all;
        }

        String empId = currentEmployee.getEmployeeId();
        return all.stream()
                .filter(r -> empId.equals(r.getEmployeeId())
                        || empId.equals(currentEmployee.getManagerId() == null ? null : r.getEmployeeId()))
                .toList();
    }

    @Override
    public SeparationRequest toggleClearance(String id, String department, boolean done, String notes) {
        SeparationRequest request = getById(id);
        boolean found = false;
        if (request.getClearances() != null) {
            for (SeparationRequest.Clearance clearance : request.getClearances()) {
                if (clearance.getDepartment().equalsIgnoreCase(department)) {
                    clearance.setDone(done);
                    if (notes != null) {
                        clearance.setNotes(notes);
                    }
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            throw new BadRequestException("No clearance item found for department: " + department);
        }
        return separationRepository.save(request);
    }

    @Override
    public SeparationRequest toggleAssetReturn(String id, String assetTag, boolean returned) {
        SeparationRequest request = getById(id);
        boolean found = false;
        if (request.getAssetReturns() != null) {
            for (SeparationRequest.AssetReturn assetReturn : request.getAssetReturns()) {
                if (assetReturn.getAssetTag().equalsIgnoreCase(assetTag)) {
                    assetReturn.setReturned(returned);
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            throw new BadRequestException("No asset return item found for tag: " + assetTag);
        }
        return separationRepository.save(request);
    }

    private boolean allClearancesDone(SeparationRequest request) {
        return request.getClearances() != null && !request.getClearances().isEmpty()
                && request.getClearances().stream().allMatch(SeparationRequest.Clearance::isDone);
    }

    @Override
    public SeparationRequest generateFullAndFinal(String id, SeparationRequest.FullAndFinal fullAndFinal) {
        SeparationRequest request = getById(id);

        if (!allClearancesDone(request)) {
            throw new BadRequestException("Cannot generate Full & Final settlement until all clearances are completed.");
        }

        double salaryTillLwd = fullAndFinal.getSalaryTillLwd() != null ? fullAndFinal.getSalaryTillLwd() : 0.0;
        double leaveEncashment = fullAndFinal.getLeaveEncashment() != null ? fullAndFinal.getLeaveEncashment() : 0.0;
        double gratuity = fullAndFinal.getGratuity() != null ? fullAndFinal.getGratuity() : 0.0;
        double recoveries = fullAndFinal.getRecoveries() != null ? fullAndFinal.getRecoveries() : 0.0;
        double netPayable = salaryTillLwd + leaveEncashment + gratuity - recoveries;

        fullAndFinal.setSalaryTillLwd(salaryTillLwd);
        fullAndFinal.setLeaveEncashment(leaveEncashment);
        fullAndFinal.setGratuity(gratuity);
        fullAndFinal.setRecoveries(recoveries);
        fullAndFinal.setNetPayable(netPayable);

        request.setFullAndFinal(fullAndFinal);
        if (!"Closed".equalsIgnoreCase(request.getStatus())) {
            request.setStatus("F&F Pending");
        }

        return separationRepository.save(request);
    }

    private boolean hasRole(String role, String expected) {
        return role != null && (role.equalsIgnoreCase(expected) || role.equalsIgnoreCase("ROLE_" + expected));
    }

    @Override
    public SeparationRequest approve(String id, String role, String remarks) {
        SeparationRequest request = getById(id);
        String currentStatus = request.getStatus();

        ApprovalAuditLog audit = ApprovalAuditLog.builder()
                .approverRole(role)
                .action("Approved")
                .timestamp(LocalDateTime.now())
                .comments(remarks != null && !remarks.isEmpty() ? remarks : "Approved")
                .build();

        String nextStatus;
        switch (currentStatus) {
            case "Manager Review":
                if (!hasRole(role, "MANAGER") && !hasRole(role, "HR") && !hasRole(role, "SUPER_ADMIN")) {
                    throw new BadRequestException("Only Manager can approve at this stage.");
                }
                nextStatus = "HR Review";
                break;
            case "HR Review":
                if (!hasRole(role, "HR") && !hasRole(role, "SUPER_ADMIN")) {
                    throw new BadRequestException("Only HR can approve at this stage.");
                }
                nextStatus = "Department Head Review";
                break;
            case "Department Head Review":
                if (!hasRole(role, "MANAGER") && !hasRole(role, "HR") && !hasRole(role, "SUPER_ADMIN")) {
                    throw new BadRequestException("Only Department Head/HR can approve at this stage.");
                }
                nextStatus = "F&F Pending";
                break;
            case "F&F Pending":
                if (!hasRole(role, "FINANCE") && !hasRole(role, "HR") && !hasRole(role, "SUPER_ADMIN")) {
                    throw new BadRequestException("Only Finance/HR can close settlement at this stage.");
                }
                if (request.getFullAndFinal() == null) {
                    throw new BadRequestException("Full & Final settlement has not been generated yet.");
                }
                nextStatus = "Closed";
                break;
            default:
                throw new BadRequestException("Separation request is already " + currentStatus + " and cannot be approved further.");
        }

        request.setStatus(nextStatus);
        if (request.getAuditLogs() == null) {
            request.setAuditLogs(new ArrayList<>());
        }
        request.getAuditLogs().add(audit);

        if ("Closed".equals(nextStatus)) {
            employeeRepository.findByEmployeeId(request.getEmployeeId())
                    .or(() -> employeeRepository.findById(request.getEmployeeId()))
                    .ifPresent(employee -> {
                        employee.setStatus("Resigned");
                        employee.setResignationDate(request.getLastWorkingDay() != null
                                ? request.getLastWorkingDay() : request.getResignationDate());
                        employeeRepository.save(employee);
                    });
        }

        return separationRepository.save(request);
    }

    @Override
    public SeparationRequest reject(String id, String role, String remarks) {
        SeparationRequest request = getById(id);

        ApprovalAuditLog audit = ApprovalAuditLog.builder()
                .approverRole(role)
                .action("Rejected")
                .timestamp(LocalDateTime.now())
                .comments(remarks != null && !remarks.isEmpty() ? remarks : "Rejected")
                .build();

        request.setStatus("Rejected");
        if (request.getAuditLogs() == null) {
            request.setAuditLogs(new ArrayList<>());
        }
        request.getAuditLogs().add(audit);

        return separationRepository.save(request);
    }
}
