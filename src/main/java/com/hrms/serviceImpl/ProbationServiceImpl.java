package com.hrms.serviceImpl;

import com.hrms.entity.ApprovalAuditLog;
import com.hrms.entity.ProbationRecord;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.ProbationRepository;
import com.hrms.service.ProbationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProbationServiceImpl implements ProbationService {

    @Autowired
    private ProbationRepository probationRepository;

    @Override
    public ProbationRecord createProbationRecord(ProbationRecord record) {
        if (record.getDateOfJoining() == null) {
            throw new BadRequestException("Date of joining is required");
        }
        int months = record.getProbationMonths() > 0 ? record.getProbationMonths() : 6;
        record.setProbationMonths(months);
        record.setConfirmationDueDate(record.getDateOfJoining().plusMonths(months));
        record.setStatus(record.getStatus() != null ? record.getStatus() : "Upcoming");
        record.setPayrollNotificationStatus("Pending");
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        if (record.getRatings() == null) {
            record.setRatings(new ArrayList<>());
        }
        if (record.getAuditLogs() == null) {
            record.setAuditLogs(new ArrayList<>());
        }
        return probationRepository.save(record);
    }

    @Override
    public List<ProbationRecord> getAll(String filter) {
        List<ProbationRecord> all = probationRepository.findAll();
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) {
            return all;
        }
        LocalDate today = LocalDate.now();
        if ("due-soon".equalsIgnoreCase(filter)) {
            LocalDate in30 = today.plusDays(30);
            return all.stream()
                    .filter(r -> r.getConfirmationDueDate() != null
                            && !r.getConfirmationDueDate().isBefore(today)
                            && !r.getConfirmationDueDate().isAfter(in30)
                            && !"Confirmed".equalsIgnoreCase(r.getStatus())
                            && !"Terminated".equalsIgnoreCase(r.getStatus()))
                    .toList();
        }
        if ("overdue".equalsIgnoreCase(filter)) {
            return all.stream()
                    .filter(r -> r.getConfirmationDueDate() != null
                            && r.getConfirmationDueDate().isBefore(today)
                            && !"Confirmed".equalsIgnoreCase(r.getStatus())
                            && !"Terminated".equalsIgnoreCase(r.getStatus()))
                    .toList();
        }
        return all;
    }

    @Override
    public ProbationRecord getById(String id) {
        return probationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Probation record not found with id: " + id));
    }

    @Override
    public ProbationRecord submitEvaluation(String id, List<ProbationRecord.RatingItem> ratings) {
        ProbationRecord record = getById(id);
        if (ratings == null || ratings.isEmpty()) {
            throw new BadRequestException("Ratings are required for evaluation");
        }
        for (ProbationRecord.RatingItem item : ratings) {
            if (item.getScore() < 1 || item.getScore() > 5) {
                throw new BadRequestException("Score must be between 1 and 5 for criterion: " + item.getCriterion());
            }
        }
        record.setRatings(ratings);
        record.setStatus("Manager Review");
        record.setUpdatedAt(LocalDateTime.now());

        if (record.getAuditLogs() == null) {
            record.setAuditLogs(new ArrayList<>());
        }
        record.getAuditLogs().add(ApprovalAuditLog.builder()
                .action("Evaluation Submitted")
                .timestamp(LocalDateTime.now())
                .comments("Manager submitted probation evaluation")
                .build());

        return probationRepository.save(record);
    }

    @Override
    public ProbationRecord submitDecision(String id, String decisionType, Integer extendByMonths, String remarks) {
        ProbationRecord record = getById(id);
        if (remarks == null || remarks.isBlank()) {
            throw new BadRequestException("Remarks are mandatory for a probation decision");
        }
        if (decisionType == null) {
            throw new BadRequestException("Decision type is required");
        }

        String normalized = decisionType.toUpperCase();
        record.setDecisionRemarks(remarks);
        record.setDecisionDate(LocalDate.now());
        record.setUpdatedAt(LocalDateTime.now());

        switch (normalized) {
            case "CONFIRM" -> {
                record.setStatus("Confirmed");
                record.setPayrollNotificationStatus("Notified");
            }
            case "EXTEND" -> {
                if (extendByMonths == null || extendByMonths <= 0) {
                    throw new BadRequestException("extendByMonths must be a positive number when extending probation");
                }
                record.setProbationMonths(record.getProbationMonths() + extendByMonths);
                LocalDate base = record.getConfirmationDueDate() != null ? record.getConfirmationDueDate() : record.getDateOfJoining();
                record.setConfirmationDueDate(base.plusMonths(extendByMonths));
                record.setStatus("Probation Extended");
            }
            case "TERMINATE" -> record.setStatus("Terminated");
            default -> throw new BadRequestException("Invalid decision type: " + decisionType);
        }

        if (record.getAuditLogs() == null) {
            record.setAuditLogs(new ArrayList<>());
        }
        record.getAuditLogs().add(ApprovalAuditLog.builder()
                .action(normalized)
                .timestamp(LocalDateTime.now())
                .comments(remarks)
                .build());

        return probationRepository.save(record);
    }
}
