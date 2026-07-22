package com.hrms.service;

import com.hrms.entity.RegularizationRequest;
import java.util.List;

public interface RegularizationService {
    RegularizationRequest apply(RegularizationRequest request);
    RegularizationRequest approve(String requestId, String remarks);
    RegularizationRequest reject(String requestId, String remarks);
    List<RegularizationRequest> getByEmployee(String employeeId);
    List<RegularizationRequest> getPendingByManager(String managerId);
}
