package com.hrms.serviceImpl;

import com.hrms.dto.EmployeeDto;
import com.hrms.entity.PreOnboardingCandidate;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.PreOnboardingRepository;
import com.hrms.service.EmployeeService;
import com.hrms.service.PreOnboardingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PreOnboardingServiceImpl implements PreOnboardingService {

    private static final String[] DEFAULT_DOCUMENTS = {
            "Aadhaar Card", "PAN Card", "Educational Certificates", "Experience Certificates",
            "Relieving Letter", "Bank Passbook", "Photo", "Passport", "Address Proof"
    };

    private static final String[] DEFAULT_IT_ADMIN = {
            "Laptop Allocation", "Email Creation", "System Credentials", "ID Card", "Access Card"
    };

    private static final String[] DEFAULT_HR = {
            "Offer Acceptance Recorded", "Policy Acknowledgement Sent", "Induction Scheduled", "Welcome Email"
    };

    @Autowired
    private PreOnboardingRepository preOnboardingRepository;

    @Autowired
    private EmployeeService employeeService;

    private List<PreOnboardingCandidate.ChecklistItem> buildChecklist(String[] items) {
        List<PreOnboardingCandidate.ChecklistItem> list = new ArrayList<>();
        for (String item : items) {
            list.add(PreOnboardingCandidate.ChecklistItem.builder().item(item).done(false).build());
        }
        return list;
    }

    @Override
    public PreOnboardingCandidate createCandidate(PreOnboardingCandidate candidate) {
        if (candidate.getName() == null || candidate.getName().trim().isEmpty()) {
            throw new BadRequestException("Error: Candidate name is required!");
        }
        if (candidate.getDocumentsChecklist() == null || candidate.getDocumentsChecklist().isEmpty()) {
            candidate.setDocumentsChecklist(buildChecklist(DEFAULT_DOCUMENTS));
        }
        if (candidate.getItAdminChecklist() == null || candidate.getItAdminChecklist().isEmpty()) {
            candidate.setItAdminChecklist(buildChecklist(DEFAULT_IT_ADMIN));
        }
        if (candidate.getHrChecklist() == null || candidate.getHrChecklist().isEmpty()) {
            candidate.setHrChecklist(buildChecklist(DEFAULT_HR));
        }
        if (candidate.getBgvStatus() == null || candidate.getBgvStatus().isEmpty()) {
            candidate.setBgvStatus("Not Started");
        }
        candidate.setStatus("In Progress");
        candidate.setId(null);
        PreOnboardingCandidate saved = preOnboardingRepository.save(candidate);
        saved.setStage(getStage(saved));
        return saved;
    }

    @Override
    public List<PreOnboardingCandidate> getAllCandidates() {
        return preOnboardingRepository.findAll().stream()
                .map(c -> {
                    c.setStage(getStage(c));
                    return c;
                })
                .collect(Collectors.toList());
    }

    @Override
    public PreOnboardingCandidate getCandidateById(String id) {
        PreOnboardingCandidate candidate = preOnboardingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pre-onboarding candidate not found with id: " + id));
        candidate.setStage(getStage(candidate));
        return candidate;
    }

    private List<PreOnboardingCandidate.ChecklistItem> toggleInList(List<PreOnboardingCandidate.ChecklistItem> items, String item, boolean done) {
        if (items == null) {
            return items;
        }
        for (PreOnboardingCandidate.ChecklistItem ci : items) {
            if (ci.getItem().equalsIgnoreCase(item)) {
                ci.setDone(done);
            }
        }
        return items;
    }

    @Override
    public PreOnboardingCandidate toggleChecklistItem(String id, String checklistType, String item, boolean done) {
        PreOnboardingCandidate candidate = preOnboardingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pre-onboarding candidate not found with id: " + id));

        switch (checklistType == null ? "" : checklistType.toLowerCase()) {
            case "documents":
                candidate.setDocumentsChecklist(toggleInList(candidate.getDocumentsChecklist(), item, done));
                break;
            case "itadmin":
            case "it_admin":
                candidate.setItAdminChecklist(toggleInList(candidate.getItAdminChecklist(), item, done));
                break;
            case "hr":
                candidate.setHrChecklist(toggleInList(candidate.getHrChecklist(), item, done));
                break;
            default:
                throw new BadRequestException("Error: Invalid checklist type. Must be one of documents, itAdmin, hr.");
        }

        PreOnboardingCandidate saved = preOnboardingRepository.save(candidate);
        saved.setStage(getStage(saved));
        return saved;
    }

    @Override
    public PreOnboardingCandidate updateBgvStatus(String id, String bgvStatus) {
        PreOnboardingCandidate candidate = preOnboardingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pre-onboarding candidate not found with id: " + id));
        candidate.setBgvStatus(bgvStatus);
        PreOnboardingCandidate saved = preOnboardingRepository.save(candidate);
        saved.setStage(getStage(saved));
        return saved;
    }

    private boolean allDone(List<PreOnboardingCandidate.ChecklistItem> items) {
        return items != null && !items.isEmpty() && items.stream().allMatch(PreOnboardingCandidate.ChecklistItem::isDone);
    }

    @Override
    public int getStage(PreOnboardingCandidate candidate) {
        // Stage 0: Offer accepted
        boolean offerAccepted = candidate.getHrChecklist() != null && candidate.getHrChecklist().stream()
                .anyMatch(i -> i.getItem().toLowerCase().contains("offer acceptance") && i.isDone());
        if (!offerAccepted) {
            return 0;
        }
        // Stage 1: Documents
        if (!allDone(candidate.getDocumentsChecklist())) {
            return 1;
        }
        // Stage 2: BGV
        if (!"Cleared".equalsIgnoreCase(candidate.getBgvStatus())) {
            return 2;
        }
        // Stage 3: IT setup
        if (!allDone(candidate.getItAdminChecklist())) {
            return 3;
        }
        // Stage 4: Ready to join
        return 4;
    }

    @Override
    public EmployeeDto convertToEmployee(String id, EmployeeDto employeeDto) {
        PreOnboardingCandidate candidate = preOnboardingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pre-onboarding candidate not found with id: " + id));

        if (employeeDto.getFirstName() == null || employeeDto.getFirstName().trim().isEmpty()) {
            String[] parts = candidate.getName() != null ? candidate.getName().trim().split("\\s+", 2) : new String[]{"", ""};
            employeeDto.setFirstName(parts.length > 0 ? parts[0] : "");
            employeeDto.setLastName(parts.length > 1 ? parts[1] : "");
        }
        if (employeeDto.getEmail() == null || employeeDto.getEmail().trim().isEmpty()) {
            employeeDto.setEmail(candidate.getEmail());
        }
        if (employeeDto.getPhone() == null) {
            employeeDto.setPhone(candidate.getPhone());
        }
        if (employeeDto.getManagerId() == null) {
            employeeDto.setManagerId(candidate.getReportingManagerId());
        }
        if (employeeDto.getJoiningDate() == null) {
            employeeDto.setJoiningDate(candidate.getDateOfJoining());
        }
        if (employeeDto.getStatus() == null) {
            employeeDto.setStatus("Active");
        }

        // Delegate the actual Employee creation to the existing EmployeeService (do not duplicate its logic)
        EmployeeDto created = employeeService.createEmployee(employeeDto);

        candidate.setConvertedEmployeeId(created.getEmployeeId());
        candidate.setStatus("Converted");
        preOnboardingRepository.save(candidate);

        return created;
    }
}
