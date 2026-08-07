package com.hrms.serviceImpl;

import com.hrms.entity.ApprovalAuditLog;
import com.hrms.entity.Employee;
import com.hrms.entity.Requisition;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.RequisitionRepository;
import com.hrms.service.RequisitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RequisitionServiceImpl implements RequisitionService {

    @Autowired
    private RequisitionRepository requisitionRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private static final String[] LEVEL_TITLES = {
            "Reporting Manager",
            "Department Head",
            "Finance",
            "HR",
            "Management"
    };

    private static final String[] LEVEL_ROLES = {
            "MANAGER",
            "MANAGER",
            "FINANCE",
            "HR",
            "SUPER_ADMIN"
    };

    @Override
    public Requisition raiseRequisition(Requisition requisition) {
        if (requisition.getRaisedByEmployeeId() == null || requisition.getRaisedByEmployeeId().isEmpty()) {
            throw new BadRequestException("raisedByEmployeeId is required to raise a requisition");
        }
        if (requisition.getNumberOfPositions() == null || requisition.getNumberOfPositions() < 1) {
            throw new BadRequestException("numberOfPositions must be at least 1");
        }

        if (requisition.getRaisedDate() == null) {
            requisition.setRaisedDate(LocalDate.now());
        }

        Employee raiser = employeeRepository.findByEmployeeId(requisition.getRaisedByEmployeeId())
                .or(() -> employeeRepository.findById(requisition.getRaisedByEmployeeId()))
                .orElse(null);

        if (raiser != null) {
            requisition.setRaisedByName(raiser.getFirstName() + " " + raiser.getLastName());

            // Auto-assign level 1 approver (Reporting Manager) if not explicitly provided
            if ((requisition.getLevel1ApproverId() == null || requisition.getLevel1ApproverId().isEmpty())
                    && raiser.getManagerId() != null && !raiser.getManagerId().isEmpty()) {
                Employee manager = employeeRepository.findByEmployeeId(raiser.getManagerId())
                        .or(() -> employeeRepository.findById(raiser.getManagerId()))
                        .orElse(null);
                if (manager != null) {
                    requisition.setLevel1ApproverId(manager.getEmployeeId());
                    requisition.setLevel1ApproverName(manager.getFirstName() + " " + manager.getLastName());
                }
            }
        }

        requisition.setTotalLevels(5);
        requisition.setCurrentLevel(1);

        requisition.setLevel1Role(LEVEL_ROLES[0]);
        requisition.setLevel1Status("Pending");
        requisition.setLevel2Role(LEVEL_ROLES[1]);
        requisition.setLevel2Status("Pending");
        requisition.setLevel3Role(LEVEL_ROLES[2]);
        requisition.setLevel3Status("Pending");
        requisition.setLevel4Role(LEVEL_ROLES[3]);
        requisition.setLevel4Status("Pending");
        requisition.setLevel5Role(LEVEL_ROLES[4]);
        requisition.setLevel5Status("Pending");

        requisition.setStatus("Pending Level 1 - " + LEVEL_TITLES[0]);

        if (requisition.getAuditLogs() == null) {
            requisition.setAuditLogs(new ArrayList<>());
        }

        return requisitionRepository.save(requisition);
    }

    private boolean roleMatches(String role, String expected) {
        if (role == null) return false;
        String normalized = role.toUpperCase().replace("ROLE_", "");
        return normalized.equals(expected);
    }

    @Override
    public Requisition approveRequisition(String id, String role, String remarks) {
        Requisition requisition = requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found with id: " + id));

        if ("Approved".equalsIgnoreCase(requisition.getStatus()) || "Rejected".equalsIgnoreCase(requisition.getStatus())) {
            throw new BadRequestException("This requisition has already been " + requisition.getStatus());
        }

        int currentLvl = requisition.getCurrentLevel() != null ? requisition.getCurrentLevel() : 1;
        String expectedRole = LEVEL_ROLES[currentLvl - 1];

        boolean isSuperAdmin = roleMatches(role, "SUPER_ADMIN");
        boolean isAuthorized = roleMatches(role, expectedRole) || isSuperAdmin;

        if (!isAuthorized) {
            throw new BadRequestException("Only " + LEVEL_TITLES[currentLvl - 1] + " (or Super Admin) can approve Level " + currentLvl + " of this requisition!");
        }

        ApprovalAuditLog audit = ApprovalAuditLog.builder()
                .approverRole(LEVEL_TITLES[currentLvl - 1])
                .action("Approved")
                .timestamp(LocalDateTime.now())
                .comments(remarks != null && !remarks.isEmpty() ? remarks : "Approved")
                .level(currentLvl)
                .build();

        setLevelStatus(requisition, currentLvl, "Approved", remarks);

        if (currentLvl >= requisition.getTotalLevels()) {
            requisition.setStatus("Approved");
        } else {
            int nextLevel = currentLvl + 1;
            requisition.setCurrentLevel(nextLevel);
            requisition.setStatus("Pending Level " + nextLevel + " - " + LEVEL_TITLES[nextLevel - 1]);
        }

        if (requisition.getAuditLogs() == null) {
            requisition.setAuditLogs(new ArrayList<>());
        }
        requisition.getAuditLogs().add(audit);

        return requisitionRepository.save(requisition);
    }

    @Override
    public Requisition rejectRequisition(String id, String role, String remarks) {
        Requisition requisition = requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found with id: " + id));

        if ("Approved".equalsIgnoreCase(requisition.getStatus()) || "Rejected".equalsIgnoreCase(requisition.getStatus())) {
            throw new BadRequestException("This requisition has already been " + requisition.getStatus());
        }

        int currentLvl = requisition.getCurrentLevel() != null ? requisition.getCurrentLevel() : 1;
        String expectedRole = LEVEL_ROLES[currentLvl - 1];

        boolean isSuperAdmin = roleMatches(role, "SUPER_ADMIN");
        boolean isAuthorized = roleMatches(role, expectedRole) || isSuperAdmin;

        if (!isAuthorized) {
            throw new BadRequestException("Only " + LEVEL_TITLES[currentLvl - 1] + " (or Super Admin) can reject Level " + currentLvl + " of this requisition!");
        }

        ApprovalAuditLog audit = ApprovalAuditLog.builder()
                .approverRole(LEVEL_TITLES[currentLvl - 1])
                .action("Rejected")
                .timestamp(LocalDateTime.now())
                .comments(remarks != null && !remarks.isEmpty() ? remarks : "Rejected")
                .level(currentLvl)
                .build();

        setLevelStatus(requisition, currentLvl, "Rejected", remarks);
        requisition.setStatus("Rejected");

        if (requisition.getAuditLogs() == null) {
            requisition.setAuditLogs(new ArrayList<>());
        }
        requisition.getAuditLogs().add(audit);

        return requisitionRepository.save(requisition);
    }

    private void setLevelStatus(Requisition requisition, int level, String status, String remarks) {
        switch (level) {
            case 1:
                requisition.setLevel1Status(status);
                requisition.setLevel1Remarks(remarks);
                break;
            case 2:
                requisition.setLevel2Status(status);
                requisition.setLevel2Remarks(remarks);
                break;
            case 3:
                requisition.setLevel3Status(status);
                requisition.setLevel3Remarks(remarks);
                break;
            case 4:
                requisition.setLevel4Status(status);
                requisition.setLevel4Remarks(remarks);
                break;
            case 5:
                requisition.setLevel5Status(status);
                requisition.setLevel5Remarks(remarks);
                break;
            default:
                throw new BadRequestException("Invalid approval level: " + level);
        }
    }

    private List<Requisition> sortDescending(List<Requisition> list) {
        List<Requisition> mutable = new ArrayList<>(list);
        mutable.sort((a, b) -> {
            if (b.getRaisedDate() == null && a.getRaisedDate() == null) return 0;
            if (b.getRaisedDate() == null) return -1;
            if (a.getRaisedDate() == null) return 1;
            return b.getRaisedDate().compareTo(a.getRaisedDate());
        });
        return mutable;
    }

    @Override
    public List<Requisition> getAllRequisitions() {
        return sortDescending(requisitionRepository.findAll());
    }

    @Override
    public List<Requisition> getPendingRequisitions() {
        List<Requisition> pending = requisitionRepository.findAll().stream()
                .filter(r -> r.getStatus() != null && r.getStatus().startsWith("Pending"))
                .toList();
        return sortDescending(pending);
    }

    @Override
    public Requisition getRequisitionById(String id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found with id: " + id));
    }

    @Override
    public List<Requisition> getRequisitionsByRequester(String employeeId) {
        return sortDescending(requisitionRepository.findByRaisedByEmployeeId(employeeId));
    }
}
