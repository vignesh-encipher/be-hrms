package com.hrms.serviceImpl;

import com.hrms.entity.BankAdviceRow;
import com.hrms.entity.Employee;
import com.hrms.entity.Payroll;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.PayrollRepository;
import com.hrms.service.BankAdviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class BankAdviceServiceImpl implements BankAdviceService {

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * period is expected in "YYYY-MM" form, e.g. "2026-08". Matches existing Payroll
     * records whose year matches and whose month equals either the numeric ("08")
     * or the full month name ("August") stored on the Payroll document, since the
     * existing Payroll.month field is a free-form String.
     */
    @Override
    public List<BankAdviceRow> getBankAdvice(String period) {
        String[] parts = period.split("-");
        int year = Integer.parseInt(parts[0]);
        int monthNum = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        String monthName = java.time.Month.of(monthNum == 0 ? 1 : monthNum)
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String monthNumStr = String.format("%02d", monthNum);

        List<Payroll> matches = payrollRepository.findAll().stream()
                .filter(p -> p.getYear() == year)
                .filter(p -> p.getMonth() != null &&
                        (p.getMonth().equalsIgnoreCase(monthName) || p.getMonth().equals(monthNumStr) || p.getMonth().equals(String.valueOf(monthNum))))
                .collect(Collectors.toList());

        return matches.stream().map(p -> {
            String employeeName = p.getEmployeeId();
            Employee employee = employeeRepository.findByEmployeeId(p.getEmployeeId()).orElse(null);
            if (employee != null) {
                employeeName = (employee.getFirstName() != null ? employee.getFirstName() : "") +
                        " " + (employee.getLastName() != null ? employee.getLastName() : "");
                employeeName = employeeName.trim();
            }
            return BankAdviceRow.builder()
                    .employeeName(employeeName)
                    .bankAccount("N/A") // Employee entity currently has no bank account field
                    .netPay(p.getNetSalary())
                    .reference("PAY-" + period + "-" + p.getEmployeeId())
                    .build();
        }).collect(Collectors.toList());
    }
}
