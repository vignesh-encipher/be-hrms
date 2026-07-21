package com.hrms.controller;

import com.hrms.dto.*;
import com.hrms.entity.ERole;
import com.hrms.entity.Employee;
import com.hrms.entity.RefreshToken;
import com.hrms.exception.BadRequestException;
import com.hrms.repository.EmployeeRepository;
import com.hrms.security.jwt.JwtUtils;
import com.hrms.security.services.RefreshTokenService;
import com.hrms.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtils.generateJwtToken(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        Optional<Employee> emp = employeeRepository.findById(userDetails.getId());
        String employeeId = emp.isPresent() ? emp.get().getEmployeeId() : null;

        return ResponseEntity.ok(JwtResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken.getToken())
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .roles(roles)
                .employeeId(employeeId)
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        if (employeeRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        Set<String> strRoles = signUpRequest.getRoles();
        Set<ERole> roles = new HashSet<>();

        if (strRoles == null) {
            roles.add(ERole.ROLE_EMPLOYEE);
        } else {
            strRoles.forEach(role -> {
                switch (role.toUpperCase()) {
                    case "SUPER_ADMIN":
                    case "ROLE_SUPER_ADMIN":
                        roles.add(ERole.ROLE_SUPER_ADMIN);
                        break;
                    case "HR":
                    case "ROLE_HR":
                        roles.add(ERole.ROLE_HR);
                        break;
                    case "MANAGER":
                    case "ROLE_MANAGER":
                        roles.add(ERole.ROLE_MANAGER);
                        break;
                    default:
                        roles.add(ERole.ROLE_EMPLOYEE);
                }
            });
        }

        long count = employeeRepository.count();
        Employee employee = Employee.builder()
                .employeeId(String.format("EMP-%03d", count + 1))
                .firstName(signUpRequest.getUsername())
                .lastName("Employee")
                .email(signUpRequest.getEmail())
                .password(encoder.encode(signUpRequest.getPassword()))
                .roles(roles)
                .status("Active")
                .employmentType("Full Time")
                .build();

        employeeRepository.save(employee);
        return ResponseEntity.ok("Employee registered successfully!");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshtoken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserId)
                .map(userId -> {
                    Employee employee = employeeRepository.findById(userId)
                            .orElseThrow(() -> new BadRequestException("Employee not found: " + userId));
                    String tokenIdentifier = employee.getEmail() != null ? employee.getEmail() : employee.getEmployeeId();
                    String token = jwtUtils.generateTokenFromUsername(tokenIdentifier);
                    return ResponseEntity.ok(new TokenRefreshResponse(token, requestRefreshToken));
                })
                .orElseThrow(() -> new BadRequestException("Refresh token is not in database!"));
    }
}
