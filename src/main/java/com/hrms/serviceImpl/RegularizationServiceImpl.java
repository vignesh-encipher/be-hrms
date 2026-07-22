package com.hrms.serviceImpl;

import com.hrms.entity.Attendance;
import com.hrms.entity.Employee;
import com.hrms.entity.RegularizationRequest;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.AttendanceRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.RegularizationRepository;
import com.hrms.service.RegularizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RegularizationServiceImpl implements RegularizationService {

    @Autowired
    private RegularizationRepository regularizationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Override
    public RegularizationRequest apply(RegularizationRequest request) {
        Employee employee = employeeRepository.findByEmployeeId(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + request.getEmployeeId()));

        // Check for existing Pending or Approved requests on the same date
        List<RegularizationRequest> existing = regularizationRepository.findByEmployeeId(request.getEmployeeId());
        boolean hasActiveRequest = existing.stream()
                .anyMatch(r -> request.getAttendanceDate().equals(r.getAttendanceDate()) 
                        && ("Pending".equalsIgnoreCase(r.getStatus()) || "Approved".equalsIgnoreCase(r.getStatus())));

        if (hasActiveRequest) {
            throw new BadRequestException("A regularization request for " + request.getAttendanceDate() + " is already pending or approved.");
        }

        if (employee.getManagerId() == null || employee.getManagerId().trim().isEmpty()) {
            // Default to admin or L1 manager if not mapped
            request.setManagerId("Admin");
        } else {
            request.setManagerId(employee.getManagerId());
        }

        request.setStatus("Pending");
        request.setSubmittedOn(LocalDateTime.now());
        return regularizationRepository.save(request);
    }

    @Override
    public RegularizationRequest approve(String requestId, String remarks) {
        RegularizationRequest request = regularizationRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Regularization request not found: " + requestId));

        if (!"Pending".equalsIgnoreCase(request.getStatus())) {
            throw new BadRequestException("Request is already processed: " + request.getStatus());
        }

        // Get manager info
        String managerName = "Manager";
        if (request.getManagerId() != null && !request.getManagerId().isEmpty()) {
            Optional<Employee> managerOpt = employeeRepository.findByEmployeeId(request.getManagerId());
            if (managerOpt.isPresent()) {
                managerName = managerOpt.get().getFirstName() + " " + managerOpt.get().getLastName();
            } else {
                managerName = request.getManagerId();
            }
        }

        request.setStatus("Approved");
        request.setApproverName(managerName);
        request.setApprovedOrRejectedOn(LocalDateTime.now());
        request.setRemarks(remarks);

        // Update or create Attendance record
        List<Attendance> existingAttendance = attendanceRepository.findByEmployeeIdAndDate(
                request.getEmployeeId(), request.getAttendanceDate()
        );

        Attendance attendance;
        if (existingAttendance.isEmpty()) {
            attendance = Attendance.builder()
                    .employeeId(request.getEmployeeId())
                    .date(request.getAttendanceDate())
                    .build();
        } else {
            attendance = existingAttendance.get(0);
        }

        attendance.setClockIn(request.getCheckInTime());
        attendance.setClockOut(request.getCheckOutTime());
        
        // Map status based on request type
        if ("Half Day Regularization".equalsIgnoreCase(request.getRequestType())) {
            attendance.setStatus("Half Day");
        } else {
            attendance.setStatus("Present");
        }

        String attendanceRemarks = "Regularized: " + request.getReason();
        if (remarks != null && !remarks.trim().isEmpty()) {
            attendanceRemarks += " (Manager comment: " + remarks + ")";
        }
        attendance.setRemarks(attendanceRemarks);

        attendanceRepository.save(attendance);
        
        // If there are other attendance chunks for this date, align their status to Present/Half Day as well
        if (existingAttendance.size() > 1) {
            for (int i = 1; i < existingAttendance.size(); i++) {
                Attendance extra = existingAttendance.get(i);
                extra.setStatus(attendance.getStatus());
                attendanceRepository.save(extra);
            }
        }

        return regularizationRepository.save(request);
    }

    @Override
    public RegularizationRequest reject(String requestId, String remarks) {
        RegularizationRequest request = regularizationRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Regularization request not found: " + requestId));

        if (!"Pending".equalsIgnoreCase(request.getStatus())) {
            throw new BadRequestException("Request is already processed: " + request.getStatus());
        }

        String managerName = "Manager";
        if (request.getManagerId() != null && !request.getManagerId().isEmpty()) {
            Optional<Employee> managerOpt = employeeRepository.findByEmployeeId(request.getManagerId());
            if (managerOpt.isPresent()) {
                managerName = managerOpt.get().getFirstName() + " " + managerOpt.get().getLastName();
            } else {
                managerName = request.getManagerId();
            }
        }

        request.setStatus("Rejected");
        request.setApproverName(managerName);
        request.setApprovedOrRejectedOn(LocalDateTime.now());
        request.setRemarks(remarks);

        return regularizationRepository.save(request);
    }

    @Override
    public List<RegularizationRequest> getByEmployee(String employeeId) {
        List<RegularizationRequest> list = regularizationRepository.findByEmployeeId(employeeId);
        list.sort((a, b) -> {
            if (b.getAttendanceDate() == null && a.getAttendanceDate() == null) return 0;
            if (b.getAttendanceDate() == null) return -1;
            if (a.getAttendanceDate() == null) return 1;
            return b.getAttendanceDate().compareTo(a.getAttendanceDate());
        });
        return list;
    }

    @Override
    public List<RegularizationRequest> getPendingByManager(String managerId) {
        List<RegularizationRequest> list = regularizationRepository.findByManagerId(managerId);
        list.sort((a, b) -> {
            if (b.getAttendanceDate() == null && a.getAttendanceDate() == null) return 0;
            if (b.getAttendanceDate() == null) return -1;
            if (a.getAttendanceDate() == null) return 1;
            return b.getAttendanceDate().compareTo(a.getAttendanceDate());
        });
        return list;
    }
}
