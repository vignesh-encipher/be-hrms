package com.hrms.service;

import com.hrms.entity.BankAdviceRow;

import java.util.List;

public interface BankAdviceService {
    List<BankAdviceRow> getBankAdvice(String period);
}
