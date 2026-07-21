package com.hrms.config;

import com.hrms.entity.*;
import com.hrms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DesignationRepository designationRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Override
    public void run(String... args) throws Exception {
        // Clear all existing data in database for a fresh start
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        designationRepository.deleteAll();
        holidayRepository.deleteAll();
        attendanceRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        payrollRepository.deleteAll();
        refreshTokenRepository.deleteAll();

        // 1. Seed Departments & Designations
        Department eng = departmentRepository.save(Department.builder().name("Engineering").code("ENG").description("Tech and Development").build());
        Department hrDept = departmentRepository.save(Department.builder().name("Human Resources").code("HR").description("Talent Acquisition and Relations").build());
        Department sales = departmentRepository.save(Department.builder().name("Sales").code("SLS").description("Customer Acquisition").build());

        Designation devDesg = designationRepository.save(Designation.builder().title("Software Engineer").departmentId(eng.getId()).description("Devs").build());
        Designation srDevDesg = designationRepository.save(Designation.builder().title("Senior Software Engineer").departmentId(eng.getId()).description("Lead Devs").build());
        Designation hrDesg = designationRepository.save(Designation.builder().title("HR Specialist").departmentId(hrDept.getId()).description("Relations").build());
        Designation salesDesg = designationRepository.save(Designation.builder().title("Sales Executive").departmentId(sales.getId()).description("Sales staff").build());

        String defaultPassword = encoder.encode("Password@123");

        // 2. Super Admin Employee
        Employee adminEmp = Employee.builder()
                .employeeId("EMP-001")
                .firstName("Super")
                .lastName("Admin")
                .email("admin@hrms.com")
                .password(defaultPassword)
                .roles(new HashSet<>(Collections.singletonList(ERole.ROLE_SUPER_ADMIN)))
                .departmentId(eng.getId())
                .designationId(srDevDesg.getId())
                .status("Active")
                .joiningDate(LocalDate.now().minusYears(2))
                .employmentType("Full Time")
                .salary(15000.00)
                .build();
        employeeRepository.save(adminEmp);

        // 3. HR Employee
        Employee hrEmp = Employee.builder()
                .employeeId("EMP-002")
                .firstName("HR")
                .lastName("Manager")
                .email("hr@hrms.com")
                .password(defaultPassword)
                .roles(new HashSet<>(Collections.singletonList(ERole.ROLE_HR)))
                .departmentId(hrDept.getId())
                .designationId(hrDesg.getId())
                .status("Active")
                .joiningDate(LocalDate.now().minusYears(1))
                .employmentType("Full Time")
                .salary(9000.00)
                .managerId("EMP-001")
                .build();
        employeeRepository.save(hrEmp);

        // 4. Manager Employee
        Employee managerEmp = Employee.builder()
                .employeeId("EMP-003")
                .firstName("Team")
                .lastName("Manager")
                .email("manager@hrms.com")
                .password(defaultPassword)
                .roles(new HashSet<>(Collections.singletonList(ERole.ROLE_MANAGER)))
                .departmentId(eng.getId())
                .designationId(srDevDesg.getId())
                .status("Active")
                .joiningDate(LocalDate.now().minusYears(3))
                .employmentType("Full Time")
                .salary(12000.00)
                .managerId("EMP-001")
                .build();
        employeeRepository.save(managerEmp);

        // 5. Regular Employee
        Employee emp = Employee.builder()
                .employeeId("EMP-004")
                .firstName("Regular")
                .lastName("Employee")
                .email("employee@hrms.com")
                .password(defaultPassword)
                .roles(new HashSet<>(Collections.singletonList(ERole.ROLE_EMPLOYEE)))
                .departmentId(eng.getId())
                .designationId(devDesg.getId())
                .status("Active")
                .joiningDate(LocalDate.now().minusMonths(6))
                .employmentType("Full Time")
                .salary(6000.00)
                .managerId("EMP-003")
                .build();
        employeeRepository.save(emp);

        // 6. Seed Holidays
        holidayRepository.save(Holiday.builder().name("New Year's Day").date(LocalDate.of(2026, 1, 1)).isrestricted(false).build());
        holidayRepository.save(Holiday.builder().name("Independence Day").date(LocalDate.of(2026, 7, 4)).isrestricted(false).build());
        holidayRepository.save(Holiday.builder().name("Christmas Day").date(LocalDate.of(2026, 12, 25)).isrestricted(false).build());
    }
}
