package com.hrms.service;

import com.hrms.entity.ProbationRecord;

import java.util.List;

public interface ProbationService {
    ProbationRecord createProbationRecord(ProbationRecord record);
    List<ProbationRecord> getAll(String filter);
    ProbationRecord getById(String id);
    ProbationRecord submitEvaluation(String id, List<ProbationRecord.RatingItem> ratings);
    ProbationRecord submitDecision(String id, String decisionType, Integer extendByMonths, String remarks);
}
