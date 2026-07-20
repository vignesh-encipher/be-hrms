package com.hrms.service;

import com.hrms.entity.Designation;
import java.util.List;

public interface DesignationService {
    List<Designation> getAllDesignations();
    Designation getDesignationById(String id);
    List<Designation> getDesignationsByDepartment(String departmentId);
    Designation createDesignation(Designation designation);
    Designation updateDesignation(String id, Designation designation);
    void deleteDesignation(String id);
}
