package com.hrms.serviceImpl;

import com.hrms.entity.LeaveRequest;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.LeaveRequestRepository;
import com.hrms.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Override
    public LeaveRequest applyLeave(LeaveRequest leaveRequest) {
        if (leaveRequest.getStartDate().isAfter(leaveRequest.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date");
        }
        
        long days = ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;
        leaveRequest.setNumberOfDays((double) days);
        leaveRequest.setStatus("Pending");
        leaveRequest.setManagerStatus("Pending");
        leaveRequest.setHrStatus("Pending");
        
        return leaveRequestRepository.save(leaveRequest);
    }

    @Override
    public LeaveRequest approveLeave(String id, String role, String remarks) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));

        if ("MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role)) {
            leaveRequest.setManagerStatus("Approved");
            leaveRequest.setManagerRemarks(remarks);
            // If HR also approved or HR status is not pending, or if it is already approved by HR
            if ("Approved".equals(leaveRequest.getHrStatus())) {
                leaveRequest.setStatus("Approved");
            }
        } else if ("HR".equalsIgnoreCase(role) || "ROLE_HR".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role) || "ROLE_SUPER_ADMIN".equalsIgnoreCase(role)) {
            leaveRequest.setHrStatus("Approved");
            leaveRequest.setHrRemarks(remarks);
            // HR approval is final or works alongside Manager approval
            leaveRequest.setStatus("Approved");
        }

        return leaveRequestRepository.save(leaveRequest);
    }

    @Override
    public LeaveRequest rejectLeave(String id, String role, String remarks) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));

        if ("MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role)) {
            leaveRequest.setManagerStatus("Rejected");
            leaveRequest.setManagerRemarks(remarks);
        } else {
            leaveRequest.setHrStatus("Rejected");
            leaveRequest.setHrRemarks(remarks);
        }
        
        leaveRequest.setStatus("Rejected");
        return leaveRequestRepository.save(leaveRequest);
    }

    @Override
    public List<LeaveRequest> getLeaveHistory(String employeeId) {
        return leaveRequestRepository.findByEmployeeId(employeeId);
    }

    @Override
    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRequestRepository.findAll();
    }

    @Override
    public List<LeaveRequest> getPendingLeaveRequests() {
        return leaveRequestRepository.findByStatus("Pending");
    }

    @Override
    public Map<String, Double> getLeaveBalance(String employeeId) {
        // Default allocations
        Map<String, Double> balance = new HashMap<>();
        balance.put("Casual Leave", 12.0);
        balance.put("Sick Leave", 10.0);
        balance.put("Paid Leave", 15.0);
        balance.put("Maternity Leave", 90.0);
        balance.put("Loss of Pay", 30.0);

        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> "Approved".equals(l.getStatus()))
                .toList();

        for (LeaveRequest request : approvedLeaves) {
            String type = request.getLeaveType();
            if (balance.containsKey(type)) {
                double current = balance.get(type);
                balance.put(type, Math.max(0, current - request.getNumberOfDays()));
            }
        }

        return balance;
    }
}
