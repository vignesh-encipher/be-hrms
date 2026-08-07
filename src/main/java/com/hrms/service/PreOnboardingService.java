package com.hrms.service;

import com.hrms.dto.EmployeeDto;
import com.hrms.entity.PreOnboardingCandidate;

import java.util.List;

public interface PreOnboardingService {
    PreOnboardingCandidate createCandidate(PreOnboardingCandidate candidate);
    List<PreOnboardingCandidate> getAllCandidates();
    PreOnboardingCandidate getCandidateById(String id);
    PreOnboardingCandidate toggleChecklistItem(String id, String checklistType, String item, boolean done);
    PreOnboardingCandidate updateBgvStatus(String id, String bgvStatus);
    int getStage(PreOnboardingCandidate candidate);
    EmployeeDto convertToEmployee(String id, EmployeeDto employeeDto);
}
