package com.hrms.serviceImpl;

import com.hrms.entity.PayrollRun;
import com.hrms.repository.PayrollRunRepository;
import com.hrms.service.PayrollRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PayrollRunServiceImpl implements PayrollRunService {

    // Stage 0: Input lock, 1: Computed, 2: Finance review, 3: HR approved, 4: Bank advice, 5: Paid
    private static final String[] STAGE_STATUS = {
            "Draft", "Processing", "Processing", "Processing", "Approved", "Paid", "Paid"
    };

    @Autowired
    private PayrollRunRepository payrollRunRepository;

    @Override
    public PayrollRun createRun(PayrollRun run) {
        if (run.getStatus() == null || run.getStatus().isEmpty()) {
            run.setStatus("Draft");
        }
        run.setStage(0);
        return payrollRunRepository.save(run);
    }

    @Override
    public List<PayrollRun> getAllRuns() {
        return payrollRunRepository.findAll();
    }

    @Override
    public PayrollRun getRunById(String id) {
        return payrollRunRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll run not found with id: " + id));
    }

    @Override
    public PayrollRun advanceStage(String id) {
        PayrollRun run = getRunById(id);
        int nextStage = Math.min(run.getStage() + 1, STAGE_STATUS.length - 1);
        run.setStage(nextStage);
        run.setStatus(STAGE_STATUS[nextStage]);
        if (nextStage == STAGE_STATUS.length - 1) {
            run.setProcessedDate(LocalDate.now());
        }
        return payrollRunRepository.save(run);
    }
}
