package com.hrms.service;

import com.hrms.entity.PayrollRun;

import java.util.List;

public interface PayrollRunService {
    PayrollRun createRun(PayrollRun run);
    List<PayrollRun> getAllRuns();
    PayrollRun getRunById(String id);
    PayrollRun advanceStage(String id);
}
