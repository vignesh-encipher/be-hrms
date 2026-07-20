package com.hrms.controller;

import com.hrms.dto.EmployeeDto;
import com.hrms.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<Page<EmployeeDto>> getAllEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "employeeId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(employeeService.getAllEmployees(search, pageable));
    }

    @GetMapping("/list")
    public ResponseEntity<List<EmployeeDto>> getAllEmployeesList() {
        return ResponseEntity.ok(employeeService.getAllEmployeesList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable String id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @GetMapping("/employeeId/{employeeId}")
    public ResponseEntity<EmployeeDto> getEmployeeByEmployeeId(@PathVariable String employeeId) {
        return ResponseEntity.ok(employeeService.getEmployeeByEmployeeId(employeeId));
    }

    @GetMapping("/userId/{userId}")
    public ResponseEntity<EmployeeDto> getEmployeeByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(employeeService.getEmployeeByUserId(userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<EmployeeDto> createEmployee(@RequestBody EmployeeDto employeeDto) {
        return ResponseEntity.ok(employeeService.createEmployee(employeeDto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR') or #id == authentication.principal.id")
    public ResponseEntity<EmployeeDto> updateEmployee(@PathVariable String id, @RequestBody EmployeeDto employeeDto) {
        // If employee is updating their own record, let them update limited fields (like photo, phone, address)
        // This validation is simplified for this demo, allowing full payload, but in production, we could restrict it.
        return ResponseEntity.ok(employeeService.updateEmployee(id, employeeDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
