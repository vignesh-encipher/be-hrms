package com.hrms.serviceImpl;

import com.hrms.dto.CalendarDayDto;
import com.hrms.dto.DayTypeResultDto;
import com.hrms.entity.Employee;
import com.hrms.entity.Holiday;
import com.hrms.entity.LeaveRequest;
import com.hrms.entity.Shift;
import com.hrms.entity.SpecialDateOverride;
import com.hrms.entity.WeekendWorkPolicy;
import com.hrms.entity.WorkCalendarAuditLog;
import com.hrms.entity.WorkCalendarRule;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.HolidayRepository;
import com.hrms.repository.LeaveRequestRepository;
import com.hrms.repository.ShiftRepository;
import com.hrms.repository.SpecialDateOverrideRepository;
import com.hrms.repository.WeekendWorkPolicyRepository;
import com.hrms.repository.WorkCalendarAuditLogRepository;
import com.hrms.repository.WorkCalendarRuleRepository;
import com.hrms.service.WorkCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WorkCalendarServiceImpl implements WorkCalendarService {

    @Autowired
    private WorkCalendarRuleRepository ruleRepository;

    @Autowired
    private SpecialDateOverrideRepository overrideRepository;

    @Autowired
    private WeekendWorkPolicyRepository weekendWorkPolicyRepository;

    @Autowired
    private WorkCalendarAuditLogRepository auditLogRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    private static final Map<Integer, String> SYSTEM_DEFAULT_PATTERN = Map.of(
            0, "OFF", 1, "WORKING", 2, "WORKING", 3, "WORKING",
            4, "WORKING", 5, "WORKING", 6, "WORKING");

    // ---------------- core resolver ----------------

    @Override
    public DayTypeResultDto resolveDayType(String employeeId, LocalDate date) {
        Employee employee = employeeRepository.findByEmployeeId(employeeId).orElse(null);

        // 1. Approved leave covering this date.
        List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeId(employeeId);
        for (LeaveRequest lr : leaves) {
            if (!"Approved".equalsIgnoreCase(lr.getStatus())) {
                continue;
            }
            if (lr.getStartDate() == null || lr.getEndDate() == null) {
                continue;
            }
            if (!date.isBefore(lr.getStartDate()) && !date.isAfter(lr.getEndDate())) {
                return DayTypeResultDto.builder()
                        .date(date).employeeId(employeeId)
                        .dayType("LEAVE").reason(lr.getLeaveType())
                        .source("LEAVE").build();
            }
        }

        // 2. Special date override, most-specific scope first.
        String departmentId = employee != null ? employee.getDepartmentId() : null;
        DayTypeResultDto overrideResult = resolveSpecialOverride(employeeId, departmentId, date);
        if (overrideResult != null) {
            return overrideResult;
        }

        // 3. Public holiday.
        List<Holiday> holidaysOnDate = holidayRepository.findByDateBetween(date, date);
        if (!holidaysOnDate.isEmpty()) {
            return DayTypeResultDto.builder()
                    .date(date).employeeId(employeeId)
                    .dayType("PUBLIC_HOLIDAY").reason(holidaysOnDate.get(0).getName())
                    .source("HOLIDAY").build();
        }

        // 4. WorkCalendarRule, EMPLOYEE -> SHIFT -> DEPARTMENT -> LOCATION(skipped, no location on Employee) -> ORGANIZATION.
        String shiftId = employee != null ? employee.getShiftId() : null;

        WorkCalendarRule rule = findActiveRule("EMPLOYEE", employeeId, date);
        String matchedScope = "EMPLOYEE";
        if (rule == null && shiftId != null) {
            rule = findActiveRule("SHIFT", shiftId, date);
            matchedScope = "SHIFT";
        }
        if (rule == null && departmentId != null) {
            rule = findActiveRule("DEPARTMENT", departmentId, date);
            matchedScope = "DEPARTMENT";
        }
        if (rule == null) {
            rule = findActiveRule("ORGANIZATION", null, date);
            matchedScope = "ORGANIZATION";
        }

        if (rule != null) {
            String dayLabel = resolvePatternForDate(rule, date);
            String dayType = "OFF".equals(dayLabel) ? "WEEKLY_OFF" : "WORKING";
            return DayTypeResultDto.builder()
                    .date(date).employeeId(employeeId)
                    .dayType(dayType).reason("Weekly pattern (" + matchedScope + ")")
                    .source("WORK_CALENDAR_RULE:" + matchedScope).build();
        }

        // 5. System default fallback (only reachable when zero configuration rows exist at all).
        int dow = date.getDayOfWeek().getValue() % 7; // 0=Sunday..6=Saturday
        String defaultLabel = SYSTEM_DEFAULT_PATTERN.getOrDefault(dow, "WORKING");
        return DayTypeResultDto.builder()
                .date(date).employeeId(employeeId)
                .dayType("OFF".equals(defaultLabel) ? "WEEKLY_OFF" : "WORKING")
                .reason("System default (Mon-Sat working, Sunday off)")
                .source("SYSTEM_DEFAULT").build();
    }

    private DayTypeResultDto resolveSpecialOverride(String employeeId, String departmentId, LocalDate date) {
        List<SpecialDateOverride> onDate = overrideRepository.findByDate(date);
        if (onDate.isEmpty()) {
            return null;
        }
        // EMPLOYEE > EMPLOYEE_GROUP(unreachable, no such entity yet) > DEPARTMENT > LOCATION(unreachable) > ORGANIZATION
        SpecialDateOverride match = pickMostSpecific(onDate, employeeId, departmentId);
        if (match == null) {
            return null;
        }
        String dayType;
        String label;
        switch (match.getDayType()) {
            case "WORKING":
                dayType = "SPECIAL_WORKING_DAY";
                label = "Special Working Day";
                break;
            case "HOLIDAY":
                dayType = "PUBLIC_HOLIDAY";
                label = match.getReason() != null ? match.getReason() : "Special Holiday";
                break;
            default:
                dayType = "WEEKLY_OFF";
                label = "Weekly Off";
        }
        return DayTypeResultDto.builder()
                .date(date).employeeId(employeeId)
                .dayType(dayType).reason(match.getReason() != null ? match.getReason() : label)
                .source("SPECIAL_DATE_OVERRIDE").build();
    }

    private SpecialDateOverride pickMostSpecific(List<SpecialDateOverride> candidates, String employeeId, String departmentId) {
        SpecialDateOverride best = null;
        int bestRank = -1;
        for (SpecialDateOverride c : candidates) {
            int rank;
            if ("EMPLOYEE".equals(c.getAppliesToScope()) && employeeId != null && employeeId.equals(c.getAppliesToRefId())) {
                rank = 4;
            } else if ("DEPARTMENT".equals(c.getAppliesToScope()) && departmentId != null && departmentId.equals(c.getAppliesToRefId())) {
                rank = 2;
            } else if ("ORGANIZATION".equals(c.getAppliesToScope())) {
                rank = 0;
            } else {
                continue; // EMPLOYEE_GROUP / LOCATION not resolvable yet - skip
            }
            if (rank > bestRank) {
                bestRank = rank;
                best = c;
            }
        }
        return best;
    }

    private WorkCalendarRule findActiveRule(String scope, String scopeRefId, LocalDate date) {
        List<WorkCalendarRule> rules = ruleRepository.findByScopeAndScopeRefId(scope, scopeRefId);
        return rules.stream()
                .filter(WorkCalendarRule::isActive)
                .filter(r -> r.getEffectiveFrom() != null && !r.getEffectiveFrom().isAfter(date))
                .filter(r -> r.getEffectiveTo() == null || !r.getEffectiveTo().isBefore(date))
                .max(Comparator.comparing(WorkCalendarRule::getEffectiveFrom))
                .orElse(null);
    }

    private String resolvePatternForDate(WorkCalendarRule rule, LocalDate date) {
        int dow = date.getDayOfWeek().getValue() % 7; // 0=Sunday..6=Saturday
        if (dow == 6 && rule.getSaturdayPattern() != null && !rule.getSaturdayPattern().isBlank()) {
            return resolveSaturdayPattern(rule.getSaturdayPattern(), date);
        }
        Map<Integer, String> pattern = rule.getWeeklyPattern();
        if (pattern != null && pattern.containsKey(dow)) {
            return pattern.get(dow);
        }
        return SYSTEM_DEFAULT_PATTERN.getOrDefault(dow, "WORKING");
    }

    private String resolveSaturdayPattern(String saturdayPattern, LocalDate date) {
        int occurrence = ((date.getDayOfMonth() - 1) / 7) + 1; // which Saturday of the month (1-based)
        switch (saturdayPattern) {
            case "EVERY_WORKING":
                return "WORKING";
            case "EVERY_OFF":
                return "OFF";
            case "FIRST_THIRD_WORKING":
                return (occurrence == 1 || occurrence == 3) ? "WORKING" : "OFF";
            case "SECOND_FOURTH_WORKING":
                return (occurrence == 2 || occurrence == 4) ? "WORKING" : "OFF";
            case "FIRST_THIRD_OFF":
                return (occurrence == 1 || occurrence == 3) ? "OFF" : "WORKING";
            case "CUSTOM":
            default:
                return "OFF";
        }
    }

    // ---------------- rule management ----------------

    @Override
    public WorkCalendarRule getEffectiveWeeklyPattern(String scope, String scopeRefId) {
        return findActiveRule(scope, scopeRefId, LocalDate.now());
    }

    @Override
    public WorkCalendarRule upsertWorkCalendarRule(WorkCalendarRule rule, String actorId) {
        if (rule.getScope() == null || rule.getScope().isBlank()) {
            throw new BadRequestException("scope is required");
        }
        if (rule.getEffectiveFrom() == null) {
            throw new BadRequestException("effectiveFrom is required");
        }

        List<WorkCalendarRule> existingRules = ruleRepository.findByScopeAndScopeRefId(rule.getScope(), rule.getScopeRefId());
        WorkCalendarRule currentActive = existingRules.stream()
                .filter(WorkCalendarRule::isActive)
                .filter(r -> r.getEffectiveTo() == null)
                .max(Comparator.comparing(WorkCalendarRule::getEffectiveFrom))
                .orElse(null);

        String previousValue = currentActive != null ? summarize(currentActive) : "(none)";

        if (currentActive != null) {
            currentActive.setEffectiveTo(rule.getEffectiveFrom().minusDays(1));
            ruleRepository.save(currentActive);
        }

        rule.setId(null);
        rule.setActive(true);
        rule.setCreatedBy(actorId);
        rule.setCreatedAt(LocalDateTime.now());
        WorkCalendarRule saved = ruleRepository.save(rule);

        writeAudit("WORK_CALENDAR_RULE", saved.getScope() + ":" + saved.getScopeRefId(),
                currentActive == null ? "CREATED" : "UPDATED",
                previousValue, summarize(saved), actorId, "Weekly pattern updated");

        return saved;
    }

    @Override
    public List<WorkCalendarRule> getRules(String scope, String scopeRefId, boolean includeHistory) {
        List<WorkCalendarRule> rules = ruleRepository.findByScopeAndScopeRefId(scope, scopeRefId);
        if (includeHistory) {
            return rules.stream().sorted(Comparator.comparing(WorkCalendarRule::getEffectiveFrom).reversed()).toList();
        }
        return rules.stream()
                .filter(r -> r.getEffectiveTo() == null)
                .filter(WorkCalendarRule::isActive)
                .toList();
    }

    private String summarize(WorkCalendarRule rule) {
        return "scope=" + rule.getScope() + ", scopeRefId=" + rule.getScopeRefId()
                + ", weeklyPattern=" + rule.getWeeklyPattern()
                + ", saturdayPattern=" + rule.getSaturdayPattern()
                + ", effectiveFrom=" + rule.getEffectiveFrom()
                + ", effectiveTo=" + rule.getEffectiveTo();
    }

    // ---------------- special date overrides ----------------

    @Override
    public SpecialDateOverride createSpecialDateOverride(SpecialDateOverride override, String actorId) {
        if (override.getDate() == null) {
            throw new BadRequestException("date is required");
        }
        if (override.getDayType() == null || override.getDayType().isBlank()) {
            throw new BadRequestException("dayType is required");
        }
        override.setId(null);
        override.setCreatedBy(actorId);
        override.setCreatedAt(LocalDateTime.now());
        SpecialDateOverride saved = overrideRepository.save(override);

        writeAudit("SPECIAL_DATE_OVERRIDE", saved.getId(), "CREATED",
                "(none)",
                "date=" + saved.getDate() + ", dayType=" + saved.getDayType()
                        + ", scope=" + saved.getAppliesToScope() + ":" + saved.getAppliesToRefId(),
                actorId, saved.getReason());

        return saved;
    }

    @Override
    public List<SpecialDateOverride> listSpecialDateOverrides(String scope, String scopeRefId, LocalDate from, LocalDate to) {
        if (scope == null || scope.isBlank()) {
            return overrideRepository.findByDateBetween(from, to);
        }
        if (scopeRefId == null || scopeRefId.isBlank()) {
            return overrideRepository.findByAppliesToScopeAndDateBetween(scope, from, to);
        }
        return overrideRepository.findByAppliesToScopeAndAppliesToRefIdAndDateBetween(scope, scopeRefId, from, to);
    }

    // ---------------- weekend work policy ----------------

    @Override
    public WeekendWorkPolicy getWeekendWorkPolicy() {
        return weekendWorkPolicyRepository.findByActiveTrue().stream()
                .max(Comparator.comparing(p -> p.getEffectiveFrom() != null ? p.getEffectiveFrom() : LocalDate.MIN))
                .orElseGet(() -> WeekendWorkPolicy.builder()
                        .treatAs("OVERTIME")
                        .requiresManagerApproval(false)
                        .requiresHrApproval(false)
                        .effectiveFrom(LocalDate.now())
                        .active(true)
                        .build());
    }

    @Override
    public WeekendWorkPolicy upsertWeekendWorkPolicy(WeekendWorkPolicy policy, String actorId) {
        List<WeekendWorkPolicy> activeOnes = weekendWorkPolicyRepository.findByActiveTrue();
        String previousValue = activeOnes.isEmpty() ? "(none)" : summarize(activeOnes.get(0));
        for (WeekendWorkPolicy p : activeOnes) {
            p.setActive(false);
            weekendWorkPolicyRepository.save(p);
        }

        policy.setId(null);
        policy.setActive(true);
        policy.setCreatedBy(actorId);
        policy.setCreatedAt(LocalDateTime.now());
        if (policy.getEffectiveFrom() == null) {
            policy.setEffectiveFrom(LocalDate.now());
        }
        WeekendWorkPolicy saved = weekendWorkPolicyRepository.save(policy);

        writeAudit("WEEKEND_POLICY", "WEEKEND_POLICY", activeOnes.isEmpty() ? "CREATED" : "UPDATED",
                previousValue, summarize(saved), actorId, "Weekend work policy updated");

        return saved;
    }

    private String summarize(WeekendWorkPolicy policy) {
        return "treatAs=" + policy.getTreatAs()
                + ", requiresManagerApproval=" + policy.isRequiresManagerApproval()
                + ", requiresHrApproval=" + policy.isRequiresHrApproval()
                + ", effectiveFrom=" + policy.getEffectiveFrom();
    }

    // ---------------- calendar grid ----------------

    @Override
    public List<CalendarDayDto> getCalendarMonth(String scope, String scopeRefId, int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        List<CalendarDayDto> days = new ArrayList<>();

        // Resolve a representative employeeId for EMPLOYEE scope; for other scopes,
        // resolve the rule/holiday/override chain directly without an employee context.
        for (int d = 1; d <= yearMonth.lengthOfMonth(); d++) {
            LocalDate date = yearMonth.atDay(d);
            CalendarDayDto dto = resolveCalendarDay(scope, scopeRefId, date);
            days.add(dto);
        }
        return days;
    }

    private CalendarDayDto resolveCalendarDay(String scope, String scopeRefId, LocalDate date) {
        // 1. Special date override for this scope.
        List<SpecialDateOverride> onDate = overrideRepository.findByDate(date);
        for (SpecialDateOverride o : onDate) {
            boolean matches = "ORGANIZATION".equals(o.getAppliesToScope())
                    || (o.getAppliesToScope() != null && o.getAppliesToScope().equals(scope)
                        && (scopeRefId == null || scopeRefId.equals(o.getAppliesToRefId())));
            if (matches) {
                String dayType = "WORKING".equals(o.getDayType()) ? "SPECIAL_WORKING_DAY"
                        : "HOLIDAY".equals(o.getDayType()) ? "PUBLIC_HOLIDAY" : "WEEKLY_OFF";
                return CalendarDayDto.builder().date(date).dayType(dayType)
                        .label(o.getReason()).source("SPECIAL_DATE_OVERRIDE").build();
            }
        }

        // 2. Public holiday.
        List<Holiday> holidays = holidayRepository.findByDateBetween(date, date);
        if (!holidays.isEmpty()) {
            return CalendarDayDto.builder().date(date).dayType("PUBLIC_HOLIDAY")
                    .label(holidays.get(0).getName()).source("HOLIDAY").build();
        }

        // 3. WorkCalendarRule for this exact scope, falling back up the hierarchy to ORGANIZATION.
        WorkCalendarRule rule = findActiveRule(scope, scopeRefId, date);
        String matchedScope = scope;
        if (rule == null && !"ORGANIZATION".equals(scope)) {
            rule = findActiveRule("ORGANIZATION", null, date);
            matchedScope = "ORGANIZATION";
        }
        if (rule != null) {
            String label = resolvePatternForDate(rule, date);
            return CalendarDayDto.builder().date(date)
                    .dayType("OFF".equals(label) ? "WEEKLY_OFF" : "WORKING")
                    .label("Weekly pattern (" + matchedScope + ")")
                    .source("WORK_CALENDAR_RULE:" + matchedScope).build();
        }

        // 4. System default fallback.
        int dow = date.getDayOfWeek().getValue() % 7;
        String defaultLabel = SYSTEM_DEFAULT_PATTERN.getOrDefault(dow, "WORKING");
        return CalendarDayDto.builder().date(date)
                .dayType("OFF".equals(defaultLabel) ? "WEEKLY_OFF" : "WORKING")
                .label("System default").source("SYSTEM_DEFAULT").build();
    }

    // ---------------- audit history ----------------

    @Override
    public List<WorkCalendarAuditLog> getHistory(String scope, String scopeRefId) {
        String key = scope + ":" + scopeRefId;
        return auditLogRepository.findAllByOrderByChangedAtDesc().stream()
                .filter(log -> key.equals(log.getEntityId()) || "WEEKEND_POLICY".equals(log.getEntityId()))
                .toList();
    }

    private void writeAudit(String entityType, String entityId, String action,
                             String previousValue, String newValue, String changedBy, String reason) {
        auditLogRepository.save(WorkCalendarAuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .previousValue(previousValue)
                .newValue(newValue)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .reason(reason)
                .build());
    }
}
