package com.hrms.serviceImpl;

import com.hrms.entity.ApprovalAuditLog;
import com.hrms.entity.Employee;
import com.hrms.entity.LeaveRequest;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.LeaveRequestRepository;
import com.hrms.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public LeaveRequest applyLeave(LeaveRequest leaveRequest) {
        if (leaveRequest.getStartDate().isAfter(leaveRequest.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date");
        }
        
        long days = ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;
        leaveRequest.setNumberOfDays((double) days);

        // Fetch applicant employee profile to dynamically determine approval chain
        Employee applicant = employeeRepository.findByEmployeeId(leaveRequest.getEmployeeId())
                .or(() -> employeeRepository.findById(leaveRequest.getEmployeeId()))
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + leaveRequest.getEmployeeId()));

        // Determine Level 1 Approver (Reporting Manager / Team Lead / HR Manager)
        String level1Id = applicant.getManagerId();
        Optional<Employee> level1Emp = (level1Id != null && !level1Id.isEmpty())
                ? employeeRepository.findByEmployeeId(level1Id)
                    .or(() -> employeeRepository.findById(level1Id))
                    .or(() -> employeeRepository.findByEmail(level1Id))
                : Optional.empty();

        // Determine Level 2 Approver (HR Approver)
        String level2Id = applicant.getHrApproverId();
        Optional<Employee> level2Emp = (level2Id != null && !level2Id.isEmpty())
                ? employeeRepository.findByEmployeeId(level2Id)
                    .or(() -> employeeRepository.findById(level2Id))
                    .or(() -> employeeRepository.findByEmail(level2Id))
                : Optional.empty();

        if (level1Emp.isPresent()) {
            leaveRequest.setLevel1ApproverId(level1Emp.get().getEmployeeId());
            leaveRequest.setLevel1ApproverName(level1Emp.get().getFirstName() + " " + level1Emp.get().getLastName());
            leaveRequest.setLevel1Role("MANAGER");
            leaveRequest.setLevel1Status("Pending");

            leaveRequest.setTotalLevels(2);
            leaveRequest.setCurrentLevel(1);
            leaveRequest.setStatus("Pending Level 1 - " + leaveRequest.getLevel1ApproverName() + " (Manager)");

            leaveRequest.setLevel2Role("HR");
            leaveRequest.setLevel2Status("Pending");
            if (level2Emp.isPresent()) {
                leaveRequest.setLevel2ApproverId(level2Emp.get().getEmployeeId());
                leaveRequest.setLevel2ApproverName(level2Emp.get().getFirstName() + " " + level2Emp.get().getLastName());
            } else {
                leaveRequest.setLevel2ApproverName("HR Department");
            }
        } else {
            // Level 1 not configured / applicable -> Skip Level 1 and set Level 2 HR as single approval
            leaveRequest.setTotalLevels(1);
            leaveRequest.setCurrentLevel(2);
            leaveRequest.setLevel1Status("Skipped");

            leaveRequest.setLevel2Role("HR");
            leaveRequest.setLevel2Status("Pending");
            if (level2Emp.isPresent()) {
                leaveRequest.setLevel2ApproverId(level2Emp.get().getEmployeeId());
                leaveRequest.setLevel2ApproverName(level2Emp.get().getFirstName() + " " + level2Emp.get().getLastName());
            } else {
                leaveRequest.setLevel2ApproverName("HR Department");
            }
            leaveRequest.setStatus("Pending Level 2 - HR Approval");
        }

        if (leaveRequest.getAuditLogs() == null) {
            leaveRequest.setAuditLogs(new ArrayList<>());
        }
        
        return leaveRequestRepository.save(leaveRequest);
    }

    @Override
    public LeaveRequest approveLeave(String id, String role, String remarks) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));

        boolean isHR = "HR".equalsIgnoreCase(role) || "ROLE_HR".equalsIgnoreCase(role);
        boolean isManager = "MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role);
        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(role) || "ROLE_SUPER_ADMIN".equalsIgnoreCase(role);

        // Rule: Super Admin is excluded from leave approvals
        if (isSuperAdmin && !isHR && !isManager) {
            throw new BadRequestException("Super Admin is not part of the leave approval workflow!");
        }

        int currentLvl = leaveRequest.getCurrentLevel() != null ? leaveRequest.getCurrentLevel() : 1;
        String approverRoleTitle = isHR ? "HR" : "Reporting Manager";

        // Record Audit Entry
        ApprovalAuditLog audit = ApprovalAuditLog.builder()
                .approverRole(approverRoleTitle)
                .action("Approved")
                .timestamp(LocalDateTime.now())
                .comments(remarks != null && !remarks.isEmpty() ? remarks : "Approved")
                .level(currentLvl)
                .build();

        if (currentLvl == 1) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = auth != null ? auth.getName() : "";
            Employee currentEmp = employeeRepository.findByEmail(currentUsername)
                    .or(() -> employeeRepository.findByEmployeeId(currentUsername))
                    .orElse(null);

            boolean isDesignatedApprover = currentEmp != null && (
                currentEmp.getEmployeeId().equals(leaveRequest.getLevel1ApproverId()) ||
                (currentEmp.getEmail() != null && currentEmp.getEmail().equals(leaveRequest.getLevel1ApproverId()))
            );

            if (!isManager && !isHR && !isDesignatedApprover) {
                throw new BadRequestException("Only designated Reporting Manager / Lead or Assigned Peer can perform Level 1 approval!");
            }
            leaveRequest.setLevel1Status("Approved");
            leaveRequest.setLevel1Remarks(remarks);

            if (leaveRequest.getTotalLevels() != null && leaveRequest.getTotalLevels() == 1) {
                leaveRequest.setStatus("Approved");
            } else {
                leaveRequest.setCurrentLevel(2);
                leaveRequest.setStatus("Pending Level 2 - HR Approval");
            }
        } else if (currentLvl == 2) {
            if (!isHR) {
                throw new BadRequestException("Only HR Approver can perform Level 2 approval!");
            }
            leaveRequest.setLevel2Status("Approved");
            leaveRequest.setLevel2Remarks(remarks);
            leaveRequest.setStatus("Approved");
        }

        if (leaveRequest.getAuditLogs() == null) {
            leaveRequest.setAuditLogs(new ArrayList<>());
        }
        leaveRequest.getAuditLogs().add(audit);

        return leaveRequestRepository.save(leaveRequest);
    }

    @Override
    public LeaveRequest rejectLeave(String id, String role, String remarks) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));

        boolean isHR = "HR".equalsIgnoreCase(role) || "ROLE_HR".equalsIgnoreCase(role);
        boolean isManager = "MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role);
        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(role) || "ROLE_SUPER_ADMIN".equalsIgnoreCase(role);

        if (isSuperAdmin && !isHR && !isManager) {
            throw new BadRequestException("Super Admin is not part of the leave approval workflow!");
        }

        int currentLvl = leaveRequest.getCurrentLevel() != null ? leaveRequest.getCurrentLevel() : 1;
        String approverRoleTitle = isHR ? "HR" : "Reporting Manager";

        ApprovalAuditLog audit = ApprovalAuditLog.builder()
                .approverRole(approverRoleTitle)
                .action("Rejected")
                .timestamp(LocalDateTime.now())
                .comments(remarks != null && !remarks.isEmpty() ? remarks : "Rejected")
                .level(currentLvl)
                .build();

        if (currentLvl == 1) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = auth != null ? auth.getName() : "";
            Employee currentEmp = employeeRepository.findByEmail(currentUsername)
                    .or(() -> employeeRepository.findByEmployeeId(currentUsername))
                    .orElse(null);

            boolean isDesignatedApprover = currentEmp != null && (
                currentEmp.getEmployeeId().equals(leaveRequest.getLevel1ApproverId()) ||
                (currentEmp.getEmail() != null && currentEmp.getEmail().equals(leaveRequest.getLevel1ApproverId()))
            );

            if (!isManager && !isHR && !isDesignatedApprover) {
                throw new BadRequestException("Only designated Reporting Manager / Lead or Assigned Peer can perform Level 1 rejection!");
            }
            leaveRequest.setLevel1Status("Rejected");
            leaveRequest.setLevel1Remarks(remarks);
        } else {
            leaveRequest.setLevel2Status("Rejected");
            leaveRequest.setLevel2Remarks(remarks);
        }

        leaveRequest.setStatus("Rejected");

        if (leaveRequest.getAuditLogs() == null) {
            leaveRequest.setAuditLogs(new ArrayList<>());
        }
        leaveRequest.getAuditLogs().add(audit);

        return leaveRequestRepository.save(leaveRequest);
    }

    @Override
    public List<LeaveRequest> getLeaveHistory(String employeeId) {
        return leaveRequestRepository.findByEmployeeId(employeeId);
    }

    @Override
    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRequestRepository.findAll();
    }

    @Override
    public List<LeaveRequest> getPendingLeaveRequests() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return java.util.Collections.emptyList();
        }
        String username = authentication.getName();
        Employee currentEmployee = employeeRepository.findByEmail(username)
                .or(() -> employeeRepository.findByEmployeeId(username))
                .orElse(null);

        List<LeaveRequest> allPending = leaveRequestRepository.findAll().stream()
                .filter(l -> l.getStatus() != null && l.getStatus().startsWith("Pending"))
                .toList();

        if (currentEmployee == null) {
            return allPending;
        }

        boolean isHR = currentEmployee.getRoles().contains(com.hrms.entity.ERole.ROLE_HR);
        boolean isSuperAdmin = currentEmployee.getRoles().contains(com.hrms.entity.ERole.ROLE_SUPER_ADMIN);

        if (isHR || isSuperAdmin) {
            return allPending; // HR/Admin see all pending requests
        }

        // For regular employees and managers: only see requests where they are L1 or L2 approver
        String empId = currentEmployee.getEmployeeId();
        String email = currentEmployee.getEmail();

        return allPending.stream()
                .filter(l -> (l.getCurrentLevel() == 1 && (empId.equals(l.getLevel1ApproverId()) || (email != null && email.equals(l.getLevel1ApproverId()))))
                          || (l.getCurrentLevel() == 2 && (empId.equals(l.getLevel2ApproverId()) || (email != null && email.equals(l.getLevel2ApproverId())))))
                .toList();
    }

    @Override
    public Map<String, Double> getLeaveBalance(String employeeId) {
        Map<String, Double> balance = new HashMap<>();
        balance.put("Casual Leave", 12.0);
        balance.put("Sick Leave", 8.0);
        balance.put("Earned Leave", 15.0);
        balance.put("Maternity Leave", 180.0);
        balance.put("Loss of Pay", 365.0);

        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> "Approved".equalsIgnoreCase(l.getStatus()))
                .toList();

        for (LeaveRequest request : approvedLeaves) {
            String type = request.getLeaveType();
            if (balance.containsKey(type)) {
                double current = balance.get(type);
                balance.put(type, Math.max(0, current - request.getNumberOfDays()));
            }
        }

        return balance;
    }
}
