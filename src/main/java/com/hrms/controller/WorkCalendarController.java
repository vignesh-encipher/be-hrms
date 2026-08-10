package com.hrms.controller;

import com.hrms.dto.CalendarDayDto;
import com.hrms.dto.DayTypeResultDto;
import com.hrms.entity.SpecialDateOverride;
import com.hrms.entity.WeekendWorkPolicy;
import com.hrms.entity.WorkCalendarAuditLog;
import com.hrms.entity.WorkCalendarRule;
import com.hrms.service.WorkCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/work-calendar")
public class WorkCalendarController {

    @Autowired
    private WorkCalendarService workCalendarService;

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }

    @GetMapping("/day-type")
    public ResponseEntity<DayTypeResultDto> getDayType(@RequestParam String employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(workCalendarService.resolveDayType(employeeId, date));
    }

    @GetMapping("/month")
    public ResponseEntity<List<CalendarDayDto>> getCalendarMonth(@RequestParam String scope,
            @RequestParam(required = false) String scopeRefId,
            @RequestParam int month, @RequestParam int year) {
        return ResponseEntity.ok(workCalendarService.getCalendarMonth(scope, scopeRefId, month, year));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<WorkCalendarRule>> getRules(@RequestParam String scope,
            @RequestParam(required = false) String scopeRefId,
            @RequestParam(defaultValue = "false") boolean includeHistory) {
        return ResponseEntity.ok(workCalendarService.getRules(scope, scopeRefId, includeHistory));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('HR') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<WorkCalendarRule> upsertRule(@RequestBody WorkCalendarRule rule) {
        return ResponseEntity.ok(workCalendarService.upsertWorkCalendarRule(rule, currentActor()));
    }

    @PostMapping("/special-dates")
    @PreAuthorize("hasRole('HR') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<SpecialDateOverride> createSpecialDate(@RequestBody SpecialDateOverride override) {
        return ResponseEntity.ok(workCalendarService.createSpecialDateOverride(override, currentActor()));
    }

    @GetMapping("/special-dates")
    public ResponseEntity<List<SpecialDateOverride>> listSpecialDates(@RequestParam(required = false) String scope,
            @RequestParam(required = false) String scopeRefId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(workCalendarService.listSpecialDateOverrides(scope, scopeRefId, from, to));
    }

    @GetMapping("/weekend-policy")
    public ResponseEntity<WeekendWorkPolicy> getWeekendPolicy() {
        return ResponseEntity.ok(workCalendarService.getWeekendWorkPolicy());
    }

    @PostMapping("/weekend-policy")
    @PreAuthorize("hasRole('HR') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<WeekendWorkPolicy> upsertWeekendPolicy(@RequestBody WeekendWorkPolicy policy) {
        return ResponseEntity.ok(workCalendarService.upsertWeekendWorkPolicy(policy, currentActor()));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('HR') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<WorkCalendarAuditLog>> getHistory(@RequestParam String scope,
            @RequestParam(required = false) String scopeRefId) {
        return ResponseEntity.ok(workCalendarService.getHistory(scope, scopeRefId));
    }
}
