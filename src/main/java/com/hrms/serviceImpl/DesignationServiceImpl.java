package com.hrms.serviceImpl;

import com.hrms.entity.Designation;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.DesignationRepository;
import com.hrms.service.DesignationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DesignationServiceImpl implements DesignationService {

    @Autowired
    private DesignationRepository designationRepository;

    @Override
    public List<Designation> getAllDesignations() {
        return designationRepository.findAll();
    }

    @Override
    public Designation getDesignationById(String id) {
        return designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + id));
    }

    @Override
    public List<Designation> getDesignationsByDepartment(String departmentId) {
        return designationRepository.findByDepartmentId(departmentId);
    }

    @Override
    public Designation createDesignation(Designation designation) {
        return designationRepository.save(designation);
    }

    @Override
    public Designation updateDesignation(String id, Designation designationDetails) {
        Designation designation = getDesignationById(id);
        designation.setTitle(designationDetails.getTitle());
        designation.setDepartmentId(designationDetails.getDepartmentId());
        designation.setDescription(designationDetails.getDescription());
        return designationRepository.save(designation);
    }

    @Override
    public void deleteDesignation(String id) {
        Designation designation = getDesignationById(id);
        designationRepository.delete(designation);
    }
}
