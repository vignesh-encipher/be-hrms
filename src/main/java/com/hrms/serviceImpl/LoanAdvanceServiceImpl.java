package com.hrms.serviceImpl;

import com.hrms.entity.LoanAdvance;
import com.hrms.repository.LoanAdvanceRepository;
import com.hrms.service.LoanAdvanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanAdvanceServiceImpl implements LoanAdvanceService {

    @Autowired
    private LoanAdvanceRepository loanAdvanceRepository;

    @Override
    public LoanAdvance createLoan(LoanAdvance loan) {
        if (loan.getStatus() == null || loan.getStatus().isEmpty()) {
            loan.setStatus("Pending approval");
        }
        loan.setInstalmentsPaid(0);
        loan.setOutstanding(loan.getAmount());
        return loanAdvanceRepository.save(loan);
    }

    @Override
    public List<LoanAdvance> getAllLoans() {
        return loanAdvanceRepository.findAll();
    }

    @Override
    public LoanAdvance getLoanById(String id) {
        return loanAdvanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan/advance not found with id: " + id));
    }

    @Override
    public LoanAdvance approveLoan(String id) {
        LoanAdvance loan = getLoanById(id);
        loan.setStatus("Recovering");
        return loanAdvanceRepository.save(loan);
    }

    @Override
    public LoanAdvance recordInstalment(String id) {
        LoanAdvance loan = getLoanById(id);
        loan.setInstalmentsPaid(loan.getInstalmentsPaid() + 1);
        double emi = loan.getEmi() != null ? loan.getEmi() : 0.0;
        double amount = loan.getAmount() != null ? loan.getAmount() : 0.0;
        double outstanding = amount - emi * loan.getInstalmentsPaid();
        if (outstanding < 0) {
            outstanding = 0;
        }
        loan.setOutstanding(outstanding);
        if (outstanding <= 0) {
            loan.setStatus("Closed");
        }
        return loanAdvanceRepository.save(loan);
    }
}
