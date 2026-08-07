package com.hrms.service;

import com.hrms.entity.Requisition;

import java.util.List;

public interface RequisitionService {
    Requisition raiseRequisition(Requisition requisition);
    Requisition approveRequisition(String id, String role, String remarks);
    Requisition rejectRequisition(String id, String role, String remarks);
    List<Requisition> getAllRequisitions();
    List<Requisition> getPendingRequisitions();
    Requisition getRequisitionById(String id);
    List<Requisition> getRequisitionsByRequester(String employeeId);
}
