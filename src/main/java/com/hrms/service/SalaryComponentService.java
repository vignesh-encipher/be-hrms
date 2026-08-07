package com.hrms.service;

import com.hrms.entity.SalaryComponent;

import java.util.List;

public interface SalaryComponentService {
    SalaryComponent createComponent(SalaryComponent component);
    List<SalaryComponent> getAllComponents();
    SalaryComponent getComponentById(String id);
    SalaryComponent updateComponent(String id, SalaryComponent component);
    void deleteComponent(String id);
}
