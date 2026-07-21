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
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DesignationRepository designationRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Departments & Designations
        if (departmentRepository.count() == 0) {
            Department eng = departmentRepository.save(Department.builder().name("Engineering").code("ENG").description("Tech and Development").build());
            Department hrDept = departmentRepository.save(Department.builder().name("Human Resources").code("HR").description("Talent Acquisition and Relations").build());
            Department sales = departmentRepository.save(Department.builder().name("Sales").code("SLS").description("Customer Acquisition").build());

            designationRepository.save(Designation.builder().title("Software Engineer").departmentId(eng.getId()).description("Devs").build());
            designationRepository.save(Designation.builder().title("Senior Software Engineer").departmentId(eng.getId()).description("Lead Devs").build());
            designationRepository.save(Designation.builder().title("HR Specialist").departmentId(hrDept.getId()).description("Relations").build());
            designationRepository.save(Designation.builder().title("Sales Executive").departmentId(sales.getId()).description("Sales staff").build());
        }

        String defaultPassword = encoder.encode("password");

        // 2. Super Admin User
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = User.builder()
                    .username("admin")
                    .email("admin@hrms.com")
                    .password(defaultPassword)
                    .roles(new HashSet<>(Collections.singletonList(ERole.ROLE_SUPER_ADMIN)))
                    .build();
            userRepository.save(adminUser);

            if (!employeeRepository.findByUserId(adminUser.getId()).isPresent()) {
                Employee adminEmp = Employee.builder()
                        .employeeId("EMP-001")
                        .firstName("Super")
                        .lastName("Admin")
                        .email("admin@hrms.com")
                        .status("Active")
                        .joiningDate(LocalDate.now().minusYears(2))
                        .employmentType("Full Time")
                        .salary(15000.00)
                        .hrApproverId("EMP-002")
                        .userId(adminUser.getId())
                        .build();
                employeeRepository.save(adminEmp);
            }
        }

        // 3. HR User
        if (!userRepository.existsByUsername("hr")) {
            User hrUser = User.builder()
                    .username("hr")
                    .email("hr@hrms.com")
                    .password(defaultPassword)
                    .roles(new HashSet<>(Collections.singletonList(ERole.ROLE_HR)))
                    .build();
            userRepository.save(hrUser);

            if (!employeeRepository.findByUserId(hrUser.getId()).isPresent()) {
                Employee hrEmp = Employee.builder()
                        .employeeId("EMP-002")
                        .firstName("HR")
                        .lastName("Manager")
                        .email("hr@hrms.com")
                        .status("Active")
                        .joiningDate(LocalDate.now().minusYears(1))
                        .employmentType("Full Time")
                        .salary(9000.00)
                        .managerId("EMP-001")
                        .hrApproverId("EMP-002")
                        .userId(hrUser.getId())
                        .build();
                employeeRepository.save(hrEmp);
            }
        }

        // 4. Manager User
        if (!userRepository.existsByUsername("manager")) {
            User managerUser = User.builder()
                    .username("manager")
                    .email("manager@hrms.com")
                    .password(defaultPassword)
                    .roles(new HashSet<>(Collections.singletonList(ERole.ROLE_MANAGER)))
                    .build();
            userRepository.save(managerUser);

            if (!employeeRepository.findByUserId(managerUser.getId()).isPresent()) {
                Employee managerEmp = Employee.builder()
                        .employeeId("EMP-003")
                        .firstName("Team")
                        .lastName("Manager")
                        .email("manager@hrms.com")
                        .status("Active")
                        .joiningDate(LocalDate.now().minusYears(3))
                        .employmentType("Full Time")
                        .salary(12000.00)
                        .managerId("EMP-001")
                        .hrApproverId("EMP-002")
                        .userId(managerUser.getId())
                        .build();
                employeeRepository.save(managerEmp);
            }
        }

        // 5. Employee User
        if (!userRepository.existsByUsername("employee")) {
            User employeeUser = User.builder()
                    .username("employee")
                    .email("employee@hrms.com")
                    .password(defaultPassword)
                    .roles(new HashSet<>(Collections.singletonList(ERole.ROLE_EMPLOYEE)))
                    .build();
            userRepository.save(employeeUser);

            if (!employeeRepository.findByUserId(employeeUser.getId()).isPresent()) {
                Employee emp = Employee.builder()
                        .employeeId("EMP-004")
                        .firstName("Regular")
                        .lastName("Employee")
                        .email("employee@hrms.com")
                        .status("Active")
                        .joiningDate(LocalDate.now().minusMonths(6))
                        .employmentType("Full Time")
                        .salary(6000.00)
                        .managerId("EMP-003")
                        .hrApproverId("EMP-002")
                        .userId(employeeUser.getId())
                        .build();
                employeeRepository.save(emp);
            }
        }

        // 6. Seed Holidays
        if (holidayRepository.count() == 0) {
            holidayRepository.save(Holiday.builder().name("New Year's Day").date(LocalDate.of(2026, 1, 1)).isrestricted(false).build());
            holidayRepository.save(Holiday.builder().name("Independence Day").date(LocalDate.of(2026, 7, 4)).isrestricted(false).build());
            holidayRepository.save(Holiday.builder().name("Christmas Day").date(LocalDate.of(2026, 12, 25)).isrestricted(false).build());
        }
    }
}
