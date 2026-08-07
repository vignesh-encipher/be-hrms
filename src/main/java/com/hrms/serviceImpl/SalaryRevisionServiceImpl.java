package com.hrms.serviceImpl;

import com.hrms.entity.SalaryRevision;
import com.hrms.repository.SalaryRevisionRepository;
import com.hrms.service.SalaryRevisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class SalaryRevisionServiceImpl implements SalaryRevisionService {

    @Autowired
    private SalaryRevisionRepository salaryRevisionRepository;

    @Override
    public SalaryRevision createRevision(SalaryRevision revision) {
        if (revision.getStatus() == null || revision.getStatus().isEmpty()) {
            revision.setStatus("Pending approval");
        }
        revision.setArrearsAmount(computeArrears(revision));
        return salaryRevisionRepository.save(revision);
    }

    private double computeArrears(SalaryRevision revision) {
        if (revision.getEffectiveDate() == null || revision.getFromCtc() == null || revision.getToCtc() == null) {
            return 0.0;
        }
        LocalDate today = LocalDate.now();
        if (revision.getEffectiveDate().isBefore(today)) {
            int monthsElapsed = Period.between(revision.getEffectiveDate(), today).getYears() * 12
                    + Period.between(revision.getEffectiveDate(), today).getMonths();
            if (monthsElapsed <= 0) {
                return 0.0;
            }
            double monthlyDelta = (revision.getToCtc() - revision.getFromCtc()) / 12.0;
            return monthsElapsed * monthlyDelta;
        }
        return 0.0;
    }

    @Override
    public List<SalaryRevision> getAllRevisions() {
        return salaryRevisionRepository.findAll();
    }

    @Override
    public SalaryRevision getRevisionById(String id) {
        return salaryRevisionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Salary revision not found with id: " + id));
    }

    @Override
    public SalaryRevision approveRevision(String id) {
        SalaryRevision revision = getRevisionById(id);
        revision.setStatus("Approved");
        return salaryRevisionRepository.save(revision);
    }
}
