package com.hrms.serviceImpl;

import com.hrms.entity.ApprovalAuditLog;
import com.hrms.entity.Employee;
import com.hrms.entity.ExpenseClaim;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.ExpenseClaimRepository;
import com.hrms.service.ExpenseClaimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExpenseClaimServiceImpl implements ExpenseClaimService {

    private static final double CERTIFICATION_ANNUAL_CAP = 15000.0;

    @Autowired
    private ExpenseClaimRepository expenseClaimRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private List<ExpenseClaim> sortDescending(List<ExpenseClaim> list) {
        List<ExpenseClaim> mutableList = new ArrayList<>(list);
        mutableList.sort((a, b) -> {
            if (b.getCreatedAt() == null && a.getCreatedAt() == null) return 0;
            if (b.getCreatedAt() == null) return -1;
            if (a.getCreatedAt() == null) return 1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        return mutableList;
    }

    private Employee currentEmployee() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        String username = auth.getName();
        return employeeRepository.findByEmail(username)
                .or(() -> employeeRepository.findByEmployeeId(username))
                .orElse(null);
    }

    private List<String> storeReceipts(MultipartFile[] receipts) {
        List<String> paths = new ArrayList<>();
        if (receipts == null) {
            return paths;
        }
        try {
            Path uploadDir = Paths.get("uploads");
            String uploadPath = uploadDir.toFile().getAbsolutePath();
            File dir = new File(uploadPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            for (MultipartFile file : receipts) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String newFilename = UUID.randomUUID().toString() + extension;
                File targetFile = new File(uploadPath + File.separator + newFilename);
                file.transferTo(targetFile);
                paths.add("/reimbursements/files/" + newFilename);
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to upload receipt: " + e.getMessage());
        }
        return paths;
    }

    private double sumApprovedCertificationAmountForYear(String employeeId, int year) {
        return expenseClaimRepository.findByEmployeeId(employeeId).stream()
                .filter(c -> "Certification".equalsIgnoreCase(c.getCategory()))
                .filter(c -> "APPROVED".equalsIgnoreCase(c.getStatus()) || "PAID".equalsIgnoreCase(c.getStatus()))
                .filter(c -> c.getExpenseDate() != null && c.getExpenseDate().getYear() == year)
                .mapToDouble(c -> c.getAmount() != null ? c.getAmount() : 0.0)
                .sum();
    }

    @Override
    public ExpenseClaim submitClaim(ExpenseClaim claim, MultipartFile[] receipts) {
        if (claim.getEmployeeId() == null || claim.getEmployeeId().isEmpty()) {
            throw new BadRequestException("employeeId is required");
        }
        if (claim.getAmount() == null || claim.getAmount() <= 0) {
            throw new BadRequestException("amount must be greater than zero");
        }

        Employee applicant = employeeRepository.findByEmployeeId(claim.getEmployeeId())
                .or(() -> employeeRepository.findById(claim.getEmployeeId()))
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + claim.getEmployeeId()));

        claim.setEmployeeId(applicant.getEmployeeId());
        claim.setEmployeeName(applicant.getFirstName() + " " + applicant.getLastName());

        if (claim.getExpenseDate() == null) {
            claim.setExpenseDate(LocalDate.now());
        }

        List<String> receiptPaths = storeReceipts(receipts);
        List<String> existing = claim.getReceiptFilePaths();
        if (existing != null && !existing.isEmpty()) {
            receiptPaths.addAll(existing);
        }
        claim.setReceiptFilePaths(receiptPaths);

        // Determine Level 1 Approver (Reporting Manager)
        String managerId = applicant.getManagerId();
        Optional<Employee> managerEmp = (managerId != null && !managerId.isEmpty())
                ? employeeRepository.findByEmployeeId(managerId)
                    .or(() -> employeeRepository.findById(managerId))
                    .or(() -> employeeRepository.findByEmail(managerId))
                : Optional.empty();

        claim.setTotalLevels(2);
        claim.setCurrentLevel(1);

        if (managerEmp.isPresent()) {
            claim.setLevel1ApproverId(managerEmp.get().getEmployeeId());
            claim.setLevel1ApproverName(managerEmp.get().getFirstName() + " " + managerEmp.get().getLastName());
            claim.setLevel1Role("MANAGER");
            claim.setLevel1Status("Pending");
            claim.setStatus("Pending - " + claim.getLevel1ApproverName());
        } else {
            claim.setLevel1Role("MANAGER");
            claim.setLevel1Status("Pending");
            claim.setLevel1ApproverName("Reporting Manager");
            claim.setStatus("Pending - Reporting Manager Approval");
        }
        claim.setLevel2Role("FINANCE");
        claim.setLevel2Status("Pending");

        // Policy check: Certification category annual cap - informational only, does not block submission
        if ("Certification".equalsIgnoreCase(claim.getCategory())) {
            int year = claim.getExpenseDate().getYear();
            double approvedSoFar = sumApprovedCertificationAmountForYear(claim.getEmployeeId(), year);
            if (approvedSoFar + claim.getAmount() > CERTIFICATION_ANNUAL_CAP) {
                claim.setPolicyBreach("Certification cap of Rs.15000/year exceeded");
            } else {
                claim.setPolicyBreach(null);
            }
        } else {
            claim.setPolicyBreach(null);
        }

        if (claim.getAuditLogs() == null) {
            claim.setAuditLogs(new ArrayList<>());
        }
        claim.setCreatedAt(LocalDateTime.now());
        claim.setUpdatedAt(LocalDateTime.now());

        return expenseClaimRepository.save(claim);
    }

    @Override
    public ExpenseClaim approve(String id, String role, String remarks) {
        ExpenseClaim claim = expenseClaimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense claim not found with id: " + id));

        boolean isFinance = "FINANCE".equalsIgnoreCase(role) || "ROLE_FINANCE".equalsIgnoreCase(role);
        boolean isManager = "MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role);
        boolean isHR = "HR".equalsIgnoreCase(role) || "ROLE_HR".equalsIgnoreCase(role);
        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(role) || "ROLE_SUPER_ADMIN".equalsIgnoreCase(role);

        int currentLvl = claim.getCurrentLevel() != null ? claim.getCurrentLevel() : 1;
        String approverRoleTitle = currentLvl == 1 ? "Reporting Manager" : "Finance";

        ApprovalAuditLog audit = ApprovalAuditLog.builder()
                .approverRole(approverRoleTitle)
                .action("Approved")
                .timestamp(LocalDateTime.now())
                .comments(remarks != null && !remarks.isEmpty() ? remarks : "Approved")
                .level(currentLvl)
                .build();

        if (currentLvl == 1) {
            Employee currentEmp = currentEmployee();
            boolean isDesignatedApprover = currentEmp != null && (
                currentEmp.getEmployeeId().equals(claim.getLevel1ApproverId()) ||
                (currentEmp.getEmail() != null && currentEmp.getEmail().equals(claim.getLevel1ApproverId()))
            );

            if (!isManager && !isHR && !isSuperAdmin && !isDesignatedApprover) {
                throw new BadRequestException("Only designated Reporting Manager can perform Level 1 approval!");
            }
            claim.setLevel1Status("Approved");
            claim.setLevel1Remarks(remarks);

            Employee financeApprover = currentEmp;
            claim.setCurrentLevel(2);
            claim.setStatus("Pending Level 2 - Finance Approval");
            if (financeApprover == null || !isFinance) {
                claim.setLevel2ApproverName("Finance Department");
            }
        } else if (currentLvl == 2) {
            if (!isFinance && !isSuperAdmin) {
                throw new BadRequestException("Only Finance Approver can perform Level 2 approval!");
            }
            claim.setLevel2Status("Approved");
            claim.setLevel2Remarks(remarks);
            claim.setStatus("APPROVED");
        } else {
            throw new BadRequestException("Expense claim is not in a state that can be approved");
        }

        if (claim.getAuditLogs() == null) {
            claim.setAuditLogs(new ArrayList<>());
        }
        claim.getAuditLogs().add(audit);
        claim.setUpdatedAt(LocalDateTime.now());

        return expenseClaimRepository.save(claim);
    }

    @Override
    public ExpenseClaim reject(String id, String role, String remarks) {
        ExpenseClaim claim = expenseClaimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense claim not found with id: " + id));

        boolean isFinance = "FINANCE".equalsIgnoreCase(role) || "ROLE_FINANCE".equalsIgnoreCase(role);
        boolean isManager = "MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role);
        boolean isHR = "HR".equalsIgnoreCase(role) || "ROLE_HR".equalsIgnoreCase(role);
        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(role) || "ROLE_SUPER_ADMIN".equalsIgnoreCase(role);

        int currentLvl = claim.getCurrentLevel() != null ? claim.getCurrentLevel() : 1;
        String approverRoleTitle = currentLvl == 1 ? "Reporting Manager" : "Finance";

        ApprovalAuditLog audit = ApprovalAuditLog.builder()
                .approverRole(approverRoleTitle)
                .action("Rejected")
                .timestamp(LocalDateTime.now())
                .comments(remarks != null && !remarks.isEmpty() ? remarks : "Rejected")
                .level(currentLvl)
                .build();

        if (currentLvl == 1) {
            Employee currentEmp = currentEmployee();
            boolean isDesignatedApprover = currentEmp != null && (
                currentEmp.getEmployeeId().equals(claim.getLevel1ApproverId()) ||
                (currentEmp.getEmail() != null && currentEmp.getEmail().equals(claim.getLevel1ApproverId()))
            );

            if (!isManager && !isHR && !isSuperAdmin && !isDesignatedApprover) {
                throw new BadRequestException("Only designated Reporting Manager can perform Level 1 rejection!");
            }
            claim.setLevel1Status("Rejected");
            claim.setLevel1Remarks(remarks);
        } else {
            if (!isFinance && !isSuperAdmin) {
                throw new BadRequestException("Only Finance Approver can perform Level 2 rejection!");
            }
            claim.setLevel2Status("Rejected");
            claim.setLevel2Remarks(remarks);
        }

        claim.setStatus("REJECTED");

        if (claim.getAuditLogs() == null) {
            claim.setAuditLogs(new ArrayList<>());
        }
        claim.getAuditLogs().add(audit);
        claim.setUpdatedAt(LocalDateTime.now());

        return expenseClaimRepository.save(claim);
    }

    @Override
    public ExpenseClaim markPaid(String id, String role) {
        ExpenseClaim claim = expenseClaimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense claim not found with id: " + id));

        boolean isFinance = "FINANCE".equalsIgnoreCase(role) || "ROLE_FINANCE".equalsIgnoreCase(role);
        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(role) || "ROLE_SUPER_ADMIN".equalsIgnoreCase(role);

        if (!isFinance && !isSuperAdmin) {
            throw new BadRequestException("Only Finance can mark an expense claim as paid!");
        }

        if (!"APPROVED".equalsIgnoreCase(claim.getStatus())) {
            throw new BadRequestException("Only approved claims can be marked as paid. Current status: " + claim.getStatus());
        }

        claim.setStatus("PAID");

        ApprovalAuditLog audit = ApprovalAuditLog.builder()
                .approverRole("Finance")
                .action("Paid")
                .timestamp(LocalDateTime.now())
                .comments("Marked as paid")
                .level(claim.getCurrentLevel())
                .build();

        if (claim.getAuditLogs() == null) {
            claim.setAuditLogs(new ArrayList<>());
        }
        claim.getAuditLogs().add(audit);
        claim.setUpdatedAt(LocalDateTime.now());

        return expenseClaimRepository.save(claim);
    }

    @Override
    public ExpenseClaim getById(String id) {
        return expenseClaimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense claim not found with id: " + id));
    }

    @Override
    public List<ExpenseClaim> getClaimHistory(String employeeId) {
        return sortDescending(expenseClaimRepository.findByEmployeeId(employeeId));
    }

    @Override
    public List<ExpenseClaim> getClaimsForRole() {
        Employee currentEmp = currentEmployee();
        List<ExpenseClaim> all = expenseClaimRepository.findAll();

        if (currentEmp == null) {
            return sortDescending(all);
        }

        boolean isHR = currentEmp.getRoles() != null && currentEmp.getRoles().contains(com.hrms.entity.ERole.ROLE_HR);
        boolean isSuperAdmin = currentEmp.getRoles() != null && currentEmp.getRoles().contains(com.hrms.entity.ERole.ROLE_SUPER_ADMIN);
        boolean isFinance = currentEmp.getRoles() != null && currentEmp.getRoles().contains(com.hrms.entity.ERole.ROLE_FINANCE);

        if (isHR || isSuperAdmin || isFinance) {
            return sortDescending(all); // HR/Admin/Finance see all expense claims
        }

        String empId = currentEmp.getEmployeeId();
        String email = currentEmp.getEmail();

        List<ExpenseClaim> filtered = all.stream()
                .filter(c -> empId.equals(c.getEmployeeId())
                          || empId.equals(c.getLevel1ApproverId())
                          || (email != null && email.equals(c.getLevel1ApproverId()))
                          || empId.equals(c.getLevel2ApproverId())
                          || (email != null && email.equals(c.getLevel2ApproverId())))
                .toList();
        return sortDescending(filtered);
    }

    @Override
    public List<ExpenseClaim> getPendingClaimsForRole() {
        Employee currentEmp = currentEmployee();

        List<ExpenseClaim> allPending = expenseClaimRepository.findAll().stream()
                .filter(c -> c.getStatus() != null && (c.getStatus().startsWith("Pending") || "PENDING".equalsIgnoreCase(c.getStatus())))
                .toList();

        if (currentEmp == null) {
            return sortDescending(allPending);
        }

        boolean isHR = currentEmp.getRoles() != null && currentEmp.getRoles().contains(com.hrms.entity.ERole.ROLE_HR);
        boolean isSuperAdmin = currentEmp.getRoles() != null && currentEmp.getRoles().contains(com.hrms.entity.ERole.ROLE_SUPER_ADMIN);
        boolean isFinance = currentEmp.getRoles() != null && currentEmp.getRoles().contains(com.hrms.entity.ERole.ROLE_FINANCE);

        if (isHR || isSuperAdmin) {
            return sortDescending(allPending);
        }

        String empId = currentEmp.getEmployeeId();
        String email = currentEmp.getEmail();

        if (isFinance) {
            List<ExpenseClaim> filtered = allPending.stream()
                    .filter(c -> c.getCurrentLevel() != null && c.getCurrentLevel() == 2)
                    .toList();
            return sortDescending(filtered);
        }

        List<ExpenseClaim> filtered = allPending.stream()
                .filter(c -> (c.getCurrentLevel() != null && c.getCurrentLevel() == 1
                        && (empId.equals(c.getLevel1ApproverId()) || (email != null && email.equals(c.getLevel1ApproverId())))))
                .toList();
        return sortDescending(filtered);
    }
}
