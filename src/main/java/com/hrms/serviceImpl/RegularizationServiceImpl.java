package com.hrms.serviceImpl;

import com.hrms.entity.Attendance;
import com.hrms.entity.AttendanceAuditLog;
import com.hrms.entity.AttendanceSession;
import com.hrms.entity.Employee;
import com.hrms.entity.RegularizationRequest;
import com.hrms.entity.Shift;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.AttendanceAuditLogRepository;
import com.hrms.repository.AttendanceRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.RegularizationRepository;
import com.hrms.service.AttendanceService;
import com.hrms.service.RegularizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceAuditLogRepository attendanceAuditLogRepository;

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

        // Get-or-create the day's Attendance doc (new session-based model).
        Optional<Attendance> existingAttendance = attendanceRepository.findByEmployeeIdAndDate(
                request.getEmployeeId(), request.getAttendanceDate()
        );

        Shift shift = attendanceService.resolveShift(request.getEmployeeId());

        Attendance attendance = existingAttendance.orElseGet(() -> Attendance.builder()
                .employeeId(request.getEmployeeId())
                .date(request.getAttendanceDate())
                .shiftId(shift.getId())
                .sessions(new ArrayList<>())
                .breaks(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build());

        String previousValue = "sessions=" + attendance.getSessions();

        // The regularization corrects the day to a single session spanning
        // checkInTime -> checkOutTime; replace whatever sessions existed with it.
        LocalDateTime correctedCheckIn = request.getCheckInTime() != null
                ? LocalDateTime.of(request.getAttendanceDate(), request.getCheckInTime()) : null;
        LocalDateTime correctedCheckOut = request.getCheckOutTime() != null
                ? LocalDateTime.of(request.getAttendanceDate(), request.getCheckOutTime()) : null;

        attendance.setSessions(new ArrayList<>());
        if (correctedCheckIn != null) {
            attendance.getSessions().add(AttendanceSession.builder()
                    .checkIn(correctedCheckIn)
                    .checkOut(correctedCheckOut)
                    .build());
        }

        // Reuse the same aggregate recomputation logic as checkOut().
        attendanceService.recomputeAggregates(attendance, shift);

        // Half Day regularizations override the computed status explicitly.
        if ("Half Day Regularization".equalsIgnoreCase(request.getRequestType())) {
            attendance.setStatus("HalfDay");
        }

        String attendanceRemarks = "Regularized: " + request.getReason();
        if (remarks != null && !remarks.trim().isEmpty()) {
            attendanceRemarks += " (Manager comment: " + remarks + ")";
        }
        attendance.setRemarks(attendanceRemarks);
        attendance.setUpdatedAt(LocalDateTime.now());

        Attendance savedAttendance = attendanceRepository.save(attendance);

        String newValue = "sessions=" + savedAttendance.getSessions();
        attendanceAuditLogRepository.save(AttendanceAuditLog.builder()
                .employeeId(request.getEmployeeId())
                .attendanceId(savedAttendance.getId())
                .action("CORRECTION_APPROVED")
                .timestamp(LocalDateTime.now())
                .previousValue(previousValue)
                .newValue(newValue)
                .reason(request.getReason())
                .performedBy(managerName)
                .build());

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

        attendanceAuditLogRepository.save(AttendanceAuditLog.builder()
                .employeeId(request.getEmployeeId())
                .attendanceId(null)
                .action("CORRECTION_REJECTED")
                .timestamp(LocalDateTime.now())
                .reason(request.getReason())
                .performedBy(managerName)
                .build());

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
