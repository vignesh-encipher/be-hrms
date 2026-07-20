package com.hrms.service;

import com.hrms.entity.Payroll;
import java.io.ByteArrayInputStream;
import java.util.List;

public interface PayrollService {
    Payroll generateSalary(Payroll payroll);
    List<Payroll> getPayrollHistory(String employeeId);
    List<Payroll> getAllPayrolls();
    byte[] generatePayslipPdf(String payrollId);
}
