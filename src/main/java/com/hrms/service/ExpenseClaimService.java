package com.hrms.service;

import com.hrms.entity.ExpenseClaim;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExpenseClaimService {
    ExpenseClaim submitClaim(ExpenseClaim claim, MultipartFile[] receipts);
    ExpenseClaim approve(String id, String role, String remarks);
    ExpenseClaim reject(String id, String role, String remarks);
    ExpenseClaim markPaid(String id, String role);
    ExpenseClaim getById(String id);
    List<ExpenseClaim> getClaimHistory(String employeeId);
    List<ExpenseClaim> getClaimsForRole();
    List<ExpenseClaim> getPendingClaimsForRole();
}
