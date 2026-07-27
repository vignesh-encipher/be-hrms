package com.hrms.serviceImpl;

import com.hrms.dto.EmployeeDto;
import com.hrms.entity.Department;
import com.hrms.entity.Designation;
import com.hrms.entity.Employee;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.DepartmentRepository;
import com.hrms.repository.DesignationRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DesignationRepository designationRepository;

    private EmployeeDto convertToDto(Employee employee) {
        EmployeeDto dto = EmployeeDto.builder()
                .id(employee.getId())
                .employeeId(employee.getEmployeeId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .gender(employee.getGender())
                .dob(employee.getDob())
                .bloodGroup(employee.getBloodGroup())
                .departmentId(employee.getDepartmentId())
                .designationId(employee.getDesignationId())
                .managerId(employee.getManagerId())
                .joiningDate(employee.getJoiningDate())
                .employmentType(employee.getEmploymentType())
                .salary(employee.getSalary())
                .address(employee.getAddress())
                .emergencyContact(employee.getEmergencyContact())
                .status(employee.getStatus())
                .photo(employee.getPhoto())
                .role(employee.getRoles() != null && !employee.getRoles().isEmpty()
                        ? employee.getRoles().iterator().next().name()
                        : "ROLE_EMPLOYEE")
                .build();

        // Resolve names
        if (employee.getDepartmentId() != null) {
            departmentRepository.findById(employee.getDepartmentId())
                    .ifPresent(dept -> dto.setDepartmentName(dept.getName()));
        }
        if (employee.getDesignationId() != null) {
            designationRepository.findById(employee.getDesignationId())
                    .ifPresent(desg -> dto.setDesignationTitle(desg.getTitle()));
        }
        if (employee.getManagerId() != null) {
            employeeRepository.findByEmployeeId(employee.getManagerId())
                    .ifPresent(mgr -> dto.setManagerName(mgr.getFirstName() + " " + mgr.getLastName()));
        }

        return dto;
    }

    private Employee convertToEntity(EmployeeDto dto) {
        return Employee.builder()
                .id(dto.getId())
                .employeeId(dto.getEmployeeId())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .gender(dto.getGender())
                .dob(dto.getDob())
                .bloodGroup(dto.getBloodGroup())
                .departmentId(dto.getDepartmentId())
                .designationId(dto.getDesignationId())
                .managerId(dto.getManagerId())
                .joiningDate(dto.getJoiningDate())
                .employmentType(dto.getEmploymentType())
                .salary(dto.getSalary())
                .address(dto.getAddress())
                .emergencyContact(dto.getEmergencyContact())
                .status(dto.getStatus())
                .photo(dto.getPhoto())
                .build();
    }

    @Override
    public Page<EmployeeDto> getAllEmployees(String search, Pageable pageable) {
        Page<Employee> page;
        if (search != null && !search.isEmpty()) {
            page = employeeRepository.searchEmployees(search, pageable);
        } else {
            page = employeeRepository.findAllActive(pageable);
        }
        return page.map(this::convertToDto);
    }

    @Override
    public List<EmployeeDto> getAllEmployeesList() {
        return employeeRepository.findAllActive().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public EmployeeDto getEmployeeById(String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        if (Boolean.TRUE.equals(employee.getDeleted())) {
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        }
        return convertToDto(employee);
    }

    @Override
    public EmployeeDto getEmployeeByEmployeeId(String employeeId) {
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with employeeId: " + employeeId));
        return convertToDto(employee);
    }

    @Override
    public EmployeeDto getEmployeeByUserId(String userId) {
        Employee employee = employeeRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + userId));
        if (Boolean.TRUE.equals(employee.getDeleted())) {
            throw new ResourceNotFoundException("Employee not found with id: " + userId);
        }
        return convertToDto(employee);
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long!");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BadRequestException("Password must contain at least one uppercase letter!");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new BadRequestException("Password must contain at least one lowercase letter!");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new BadRequestException("Password must contain at least one number!");
        }
        if (!password.matches(".*[!@#$%^&*(),.?\"':{}|<>].*")) {
            throw new BadRequestException("Password must contain at least one special character!");
        }
    }

    @Override
    public EmployeeDto createEmployee(EmployeeDto employeeDto) {
        if (employeeDto.getEmail() == null || employeeDto.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Error: Email Address is required!");
        }

        String email = employeeDto.getEmail().trim();

        if (employeeRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("Error: Email is already in use!");
        }

        validatePassword(employeeDto.getPassword());

        // Parse role
        com.hrms.entity.ERole roleEnum = com.hrms.entity.ERole.ROLE_EMPLOYEE;
        if (employeeDto.getRole() != null && !employeeDto.getRole().isEmpty()) {
            String r = employeeDto.getRole().toUpperCase();
            if (r.contains("SUPER") || r.contains("SUPER_ADMIN")) {
                roleEnum = com.hrms.entity.ERole.ROLE_SUPER_ADMIN;
            } else if (r.contains("HR")) {
                roleEnum = com.hrms.entity.ERole.ROLE_HR;
            } else if (r.contains("MANAGER")) {
                roleEnum = com.hrms.entity.ERole.ROLE_MANAGER;
            } else if (r.contains("EMPLOYEE")) {
                roleEnum = com.hrms.entity.ERole.ROLE_EMPLOYEE;
            }
        }

        Employee employee = convertToEntity(employeeDto);
        employee.setEmail(email);
        employee.setPassword(passwordEncoder.encode(employeeDto.getPassword()));
        employee.setRoles(java.util.Set.of(roleEnum));
        employee.setIsFirstLogin(true);

        if (employee.getEmployeeId() == null || employee.getEmployeeId().isEmpty()) {
            long count = employeeRepository.count();
            employee.setEmployeeId(String.format("EMP-%03d", count + 1));
        }
        Employee saved = employeeRepository.save(employee);
        return convertToDto(saved);
    }

    @Override
    public EmployeeDto updateEmployee(String id, EmployeeDto employeeDto) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        
        if (employeeDto.getEmail() != null && !employeeDto.getEmail().isEmpty()
                && !employeeDto.getEmail().equalsIgnoreCase(existing.getEmail())) {
            if (employeeRepository.findByEmail(employeeDto.getEmail()).isPresent()) {
                throw new BadRequestException("Error: Email is already in use!");
            }
        }
        
        existing.setFirstName(employeeDto.getFirstName());
        existing.setLastName(employeeDto.getLastName());
        existing.setEmail(employeeDto.getEmail());
        existing.setPhone(employeeDto.getPhone());
        existing.setGender(employeeDto.getGender());
        existing.setDob(employeeDto.getDob());
        existing.setBloodGroup(employeeDto.getBloodGroup());
        existing.setDepartmentId(employeeDto.getDepartmentId());
        existing.setDesignationId(employeeDto.getDesignationId());
        existing.setManagerId(employeeDto.getManagerId());
        existing.setJoiningDate(employeeDto.getJoiningDate());
        existing.setEmploymentType(employeeDto.getEmploymentType());
        existing.setSalary(employeeDto.getSalary());
        existing.setAddress(employeeDto.getAddress());
        existing.setEmergencyContact(employeeDto.getEmergencyContact());
        existing.setStatus(employeeDto.getStatus());
        if (employeeDto.getPhoto() != null) {
            existing.setPhoto(employeeDto.getPhoto());
        }

        // Update role if provided
        if (employeeDto.getRole() != null && !employeeDto.getRole().isEmpty()) {
            com.hrms.entity.ERole roleEnum = com.hrms.entity.ERole.ROLE_EMPLOYEE;
            String r = employeeDto.getRole().toUpperCase();
            if (r.contains("SUPER") || r.contains("SUPER_ADMIN")) {
                roleEnum = com.hrms.entity.ERole.ROLE_SUPER_ADMIN;
            } else if (r.contains("HR")) {
                roleEnum = com.hrms.entity.ERole.ROLE_HR;
            } else if (r.contains("MANAGER")) {
                roleEnum = com.hrms.entity.ERole.ROLE_MANAGER;
            } else if (r.contains("EMPLOYEE")) {
                roleEnum = com.hrms.entity.ERole.ROLE_EMPLOYEE;
            }
            existing.setRoles(java.util.Set.of(roleEnum));
        }

        Employee saved = employeeRepository.save(existing);
        return convertToDto(saved);
    }

    @Override
    public void deleteEmployee(String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employee.setDeleted(true);
        employeeRepository.save(employee);
    }

    @Override
    public byte[] getEmployeeTemplateExcel() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employees Template");
            
            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "First Name *", "Last Name *", "Email *", "Phone", "Gender", "DOB (YYYY-MM-DD)", 
                "Blood Group", "Department ID", "Designation ID", "Manager ID", "Employment Type", 
                "Salary", "Role (EMPLOYEE/MANAGER/HR) *", "Status (Active/On Leave)"
            };
            
            CellStyle headerCellStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerCellStyle.setFont(headerFont);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Add a sample dummy row
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("John");
            sampleRow.createCell(1).setCellValue("Doe");
            sampleRow.createCell(2).setCellValue("john.doe@example.com");
            sampleRow.createCell(3).setCellValue("1234567890");
            sampleRow.createCell(4).setCellValue("Male");
            sampleRow.createCell(5).setCellValue("1995-05-15");
            sampleRow.createCell(6).setCellValue("O+");
            sampleRow.createCell(7).setCellValue("ENG");
            sampleRow.createCell(8).setCellValue("Software Engineer");
            sampleRow.createCell(9).setCellValue("EMP-001");
            sampleRow.createCell(10).setCellValue("Full Time");
            sampleRow.createCell(11).setCellValue(5000.0);
            sampleRow.createCell(12).setCellValue("EMPLOYEE");
            sampleRow.createCell(13).setCellValue("Active");

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Fail to generate Excel template file: " + e.getMessage(), e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double numValue = cell.getNumericCellValue();
                if (numValue == (long) numValue) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    @Override
    public Map<String, Object> uploadBulkEmployees(MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            
            // Skip Header
            if (rows.hasNext()) {
                rows.next();
            }
            
            long employeeCount = employeeRepository.count();
            String defaultPassword = passwordEncoder.encode("Password@123");

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                
                // Check if the row is empty
                String firstName = getCellValueAsString(currentRow.getCell(0));
                String lastName = getCellValueAsString(currentRow.getCell(1));
                String email = getCellValueAsString(currentRow.getCell(2));
                
                if (firstName.isEmpty() && lastName.isEmpty() && email.isEmpty()) {
                    continue; // Skip empty rows
                }
                
                try {
                    if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                        throw new BadRequestException("First Name, Last Name, and Email are required fields.");
                    }
                    
                    if (employeeRepository.findByEmail(email).isPresent()) {
                        throw new BadRequestException("Email '" + email + "' is already in use.");
                    }

                    String phone = getCellValueAsString(currentRow.getCell(3));
                    String gender = getCellValueAsString(currentRow.getCell(4));
                    String dobStr = getCellValueAsString(currentRow.getCell(5));
                    LocalDate dob = null;
                    if (!dobStr.isEmpty()) {
                        try {
                            dob = LocalDate.parse(dobStr, DateTimeFormatter.ISO_LOCAL_DATE);
                        } catch (Exception ex) {
                            throw new BadRequestException("Invalid DOB format. Must be YYYY-MM-DD.");
                        }
                    }

                    String bloodGroup = getCellValueAsString(currentRow.getCell(6));
                    String departmentId = getCellValueAsString(currentRow.getCell(7));
                    String designationId = getCellValueAsString(currentRow.getCell(8));
                    String managerId = getCellValueAsString(currentRow.getCell(9));
                    String employmentType = getCellValueAsString(currentRow.getCell(10));
                    
                    double salary = 0;
                    String salaryStr = getCellValueAsString(currentRow.getCell(11));
                    if (!salaryStr.isEmpty()) {
                        try {
                            salary = Double.parseDouble(salaryStr);
                        } catch (Exception ex) {
                            throw new BadRequestException("Invalid Salary. Must be a numeric value.");
                        }
                    }

                    String roleStr = getCellValueAsString(currentRow.getCell(12)).toUpperCase();
                    com.hrms.entity.ERole roleEnum = com.hrms.entity.ERole.ROLE_EMPLOYEE;
                    if (roleStr.contains("SUPER")) {
                        roleEnum = com.hrms.entity.ERole.ROLE_SUPER_ADMIN;
                    } else if (roleStr.contains("HR")) {
                        roleEnum = com.hrms.entity.ERole.ROLE_HR;
                    } else if (roleStr.contains("MANAGER")) {
                        roleEnum = com.hrms.entity.ERole.ROLE_MANAGER;
                    }

                    String status = getCellValueAsString(currentRow.getCell(13));
                    if (status.isEmpty()) {
                        status = "Active";
                    }

                    employeeCount++;
                    Employee employee = Employee.builder()
                            .employeeId(String.format("EMP-%03d", employeeCount))
                            .firstName(firstName)
                            .lastName(lastName)
                            .email(email)
                            .phone(phone)
                            .gender(gender)
                            .dob(dob)
                            .bloodGroup(bloodGroup)
                            .departmentId(departmentId)
                            .designationId(designationId)
                            .managerId(managerId)
                            .joiningDate(LocalDate.now())
                            .employmentType(employmentType.isEmpty() ? "Full Time" : employmentType)
                            .salary(salary)
                            .password(defaultPassword)
                            .roles(java.util.Set.of(roleEnum))
                            .status(status)
                            .build();

                    employeeRepository.save(employee);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add("Row " + (currentRow.getRowNum() + 1) + " (" + (email.isEmpty() ? "No Email" : email) + "): " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage(), e);
        }

        response.put("successCount", successCount);
        response.put("failCount", failCount);
        response.put("errors", errors);
        return response;
    }
}
