package com.hrms.service;

import com.hrms.entity.LeaveRequest;
import java.util.List;
import java.util.Map;

public interface LeaveService {
    LeaveRequest applyLeave(LeaveRequest leaveRequest);
    LeaveRequest approveLeave(String id, String role, String remarks);
    LeaveRequest rejectLeave(String id, String role, String remarks);
    List<LeaveRequest> getLeaveHistory(String employeeId);
    List<LeaveRequest> getAllLeaveRequests();
    List<LeaveRequest> getPendingLeaveRequests();
    Map<String, Double> getLeaveBalance(String employeeId);
}
