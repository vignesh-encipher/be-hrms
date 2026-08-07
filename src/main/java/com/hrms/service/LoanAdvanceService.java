package com.hrms.service;

import com.hrms.entity.LoanAdvance;

import java.util.List;

public interface LoanAdvanceService {
    LoanAdvance createLoan(LoanAdvance loan);
    List<LoanAdvance> getAllLoans();
    LoanAdvance getLoanById(String id);
    LoanAdvance approveLoan(String id);
    LoanAdvance recordInstalment(String id);
}
