package com.hrms.serviceImpl;

import com.hrms.entity.SalaryComponent;
import com.hrms.repository.SalaryComponentRepository;
import com.hrms.service.SalaryComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalaryComponentServiceImpl implements SalaryComponentService {

    @Autowired
    private SalaryComponentRepository salaryComponentRepository;

    @Override
    public SalaryComponent createComponent(SalaryComponent component) {
        return salaryComponentRepository.save(component);
    }

    @Override
    public List<SalaryComponent> getAllComponents() {
        return salaryComponentRepository.findAll();
    }

    @Override
    public SalaryComponent getComponentById(String id) {
        return salaryComponentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Salary component not found with id: " + id));
    }

    @Override
    public SalaryComponent updateComponent(String id, SalaryComponent component) {
        SalaryComponent existing = getComponentById(id);
        existing.setName(component.getName());
        existing.setType(component.getType());
        existing.setCalculationDescription(component.getCalculationDescription());
        existing.setAppliesTo(component.getAppliesTo());
        existing.setStatutory(component.isStatutory());
        existing.setPartOfCtc(component.isPartOfCtc());
        return salaryComponentRepository.save(existing);
    }

    @Override
    public void deleteComponent(String id) {
        salaryComponentRepository.deleteById(id);
    }
}
