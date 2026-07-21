package com.hrms.controller;

import com.hrms.dto.OrgTreeNodeDto;
import com.hrms.dto.OrgTreeUpdateRequest;
import com.hrms.entity.Department;
import com.hrms.entity.Designation;
import com.hrms.entity.Employee;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.DepartmentRepository;
import com.hrms.repository.DesignationRepository;
import com.hrms.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/organization")
public class OrganizationController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DesignationRepository designationRepository;

    @GetMapping("/tree")
    public ResponseEntity<List<OrgTreeNodeDto>> getOrganizationTree() {
        List<Employee> allEmployees = employeeRepository.findAll();
        Map<String, String> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));
        Map<String, String> desigMap = designationRepository.findAll().stream()
                .collect(Collectors.toMap(Designation::getId, Designation::getTitle, (a, b) -> a));

        Map<String, Employee> empByIdMap = allEmployees.stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, e -> e, (a, b) -> a));

        Map<String, OrgTreeNodeDto> dtoMap = new HashMap<>();

        for (Employee emp : allEmployees) {
            String name = ((emp.getFirstName() != null ? emp.getFirstName() : "") + " " + (emp.getLastName() != null ? emp.getLastName() : "")).trim();
            String deptName = emp.getDepartmentId() != null ? deptMap.getOrDefault(emp.getDepartmentId(), "General") : "General";
            String desigTitle = emp.getDesignationId() != null ? desigMap.getOrDefault(emp.getDesignationId(), "Team Member") : "Team Member";

            String mgrName = "None";
            if (emp.getManagerId() != null && empByIdMap.containsKey(emp.getManagerId())) {
                Employee m = empByIdMap.get(emp.getManagerId());
                mgrName = (m.getFirstName() + " " + m.getLastName()).trim();
            }

            String hrName = "HR Team";
            if (emp.getHrApproverId() != null && empByIdMap.containsKey(emp.getHrApproverId())) {
                Employee h = empByIdMap.get(emp.getHrApproverId());
                hrName = (h.getFirstName() + " " + h.getLastName()).trim();
            }

            OrgTreeNodeDto dto = OrgTreeNodeDto.builder()
                    .id(emp.getId())
                    .key(emp.getEmployeeId())
                    .title(name)
                    .employeeId(emp.getEmployeeId())
                    .firstName(emp.getFirstName())
                    .lastName(emp.getLastName())
                    .name(name)
                    .email(emp.getEmail())
                    .phone(emp.getPhone())
                    .designation(desigTitle)
                    .department(deptName)
                    .managerId(emp.getManagerId())
                    .managerName(mgrName)
                    .hrApproverId(emp.getHrApproverId())
                    .hrApproverName(hrName)
                    .status(emp.getStatus() != null ? emp.getStatus() : "Active")
                    .photo(emp.getPhoto())
                    .joiningDate(emp.getJoiningDate() != null ? emp.getJoiningDate().toString() : null)
                    .salary(emp.getSalary())
                    .children(new ArrayList<>())
                    .build();

            dtoMap.put(emp.getEmployeeId(), dto);
        }

        // Build tree hierarchy
        List<OrgTreeNodeDto> rootNodes = new ArrayList<>();
        for (OrgTreeNodeDto node : dtoMap.values()) {
            if (node.getManagerId() != null && dtoMap.containsKey(node.getManagerId()) && !node.getManagerId().equalsIgnoreCase(node.getEmployeeId())) {
                OrgTreeNodeDto parent = dtoMap.get(node.getManagerId());
                parent.getChildren().add(node);
            } else {
                rootNodes.add(node);
            }
        }

        // Calculate direct reports count recursively
        for (OrgTreeNodeDto node : dtoMap.values()) {
            node.setDirectReportsCount(node.getChildren().size());
        }

        return ResponseEntity.ok(rootNodes);
    }

    @PutMapping("/tree")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> updateOrganizationTree(@RequestBody OrgTreeUpdateRequest request) {
        if (request.getEmployeeId() == null || request.getEmployeeId().isEmpty()) {
            throw new BadRequestException("Employee ID is required");
        }

        if (request.getNewManagerId() != null && request.getNewManagerId().equalsIgnoreCase(request.getEmployeeId())) {
            throw new BadRequestException("An employee cannot report to themselves");
        }

        Employee emp = employeeRepository.findByEmployeeId(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.getEmployeeId()));

        // Circular hierarchy validation check
        if (request.getNewManagerId() != null && !request.getNewManagerId().isEmpty()) {
            String curr = request.getNewManagerId();
            while (curr != null && !curr.isEmpty()) {
                if (curr.equalsIgnoreCase(request.getEmployeeId())) {
                    throw new BadRequestException("Circular reporting hierarchy detected! Cannot set direct report as manager.");
                }
                Optional<Employee> parentOpt = employeeRepository.findByEmployeeId(curr);
                curr = parentOpt.isPresent() ? parentOpt.get().getManagerId() : null;
            }
        }

        emp.setManagerId(request.getNewManagerId());
        if (request.getNewHrApproverId() != null) {
            emp.setHrApproverId(request.getNewHrApproverId());
        }
        employeeRepository.save(emp);

        return ResponseEntity.ok("Organization hierarchy updated successfully");
    }

    @GetMapping("/approval-config/{employeeId}")
    public ResponseEntity<Map<String, String>> getApprovalConfig(@PathVariable String employeeId) {
        Employee emp = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        Map<String, String> config = new HashMap<>();
        config.put("employeeId", emp.getEmployeeId());
        config.put("managerId", emp.getManagerId());
        config.put("hrApproverId", emp.getHrApproverId());

        return ResponseEntity.ok(config);
    }

    @PutMapping("/approval-config/{employeeId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<Map<String, String>> updateApprovalConfig(
            @PathVariable String employeeId,
            @RequestBody Map<String, String> payload) {
        Employee emp = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        if (payload.containsKey("managerId")) {
            emp.setManagerId(payload.get("managerId"));
        }
        if (payload.containsKey("hrApproverId")) {
            emp.setHrApproverId(payload.get("hrApproverId"));
        }
        employeeRepository.save(emp);

        Map<String, String> result = new HashMap<>();
        result.put("employeeId", emp.getEmployeeId());
        result.put("managerId", emp.getManagerId());
        result.put("hrApproverId", emp.getHrApproverId());
        result.put("message", "Approval configuration updated successfully");

        return ResponseEntity.ok(result);
    }
}
