package com.hrms.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAdviceRow {
    private String employeeName;
    private String bankAccount;
    private Double netPay;
    private String reference;
}
