package com.hrms.serviceImpl;

import com.hrms.dto.AttendanceStatusDto;
import com.hrms.dto.MonthlyAttendanceSummaryDto;
import com.hrms.dto.MonthlyRegisterRowDto;
import com.hrms.dto.TeamAttendanceDto;
import com.hrms.entity.Attendance;
import com.hrms.entity.AttendanceAuditLog;
import com.hrms.entity.AttendanceBreak;
import com.hrms.entity.AttendanceSession;
import com.hrms.entity.Employee;
import com.hrms.entity.Holiday;
import com.hrms.entity.LeaveRequest;
import com.hrms.entity.Shift;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.AttendanceAuditLogRepository;
import com.hrms.repository.AttendanceRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.HolidayRepository;
import com.hrms.repository.LeaveRequestRepository;
import com.hrms.repository.ShiftRepository;
import com.hrms.service.AttendanceService;
import com.hrms.service.WorkCalendarService;
import com.hrms.dto.DayTypeResultDto;
import com.hrms.entity.WeekendWorkPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceAuditLogRepository auditLogRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private WorkCalendarService workCalendarService;

    /**
     * Sets Attendance.dayType (WORKING/WEEKLY_OFF/PUBLIC_HOLIDAY/SPECIAL_WORKING_DAY/LEAVE)
     * the first time this day's document is touched, either at check-in or lazily
     * on read (getMonthlyRegister/getHistory/getTodayStatus), so no separate nightly
     * job is needed. Does not overwrite an already-set dayType.
     */
    private String ensureDayType(Attendance attendance) {
        if (attendance.getDayType() != null && !attendance.getDayType().isBlank()) {
            return attendance.getDayType();
        }
        DayTypeResultDto result = workCalendarService.resolveDayType(attendance.getEmployeeId(), attendance.getDate());
        attendance.setDayType(result.getDayType());
        return result.getDayType();
    }

    // ---------- shift resolution ----------

    @Override
    public Shift resolveShift(String employeeId) {
        Optional<Employee> empOpt = employeeRepository.findByEmployeeId(employeeId);
        String shiftId = empOpt.map(Employee::getShiftId).orElse(null);
        if (shiftId != null && !shiftId.trim().isEmpty()) {
            Optional<Shift> shiftOpt = shiftRepository.findById(shiftId);
            if (shiftOpt.isPresent()) {
                return shiftOpt.get();
            }
        }
        return shiftRepository.findByName("General")
                .orElseGet(() -> shiftRepository.findAll().stream().findFirst()
                        .orElseGet(() -> Shift.builder()
                                .name("General")
                                .startTime(LocalTime.of(9, 0))
                                .endTime(LocalTime.of(18, 0))
                                .build()));
    }

    private Employee requireActiveEmployee(String employeeId) {
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));
        if (employee.getStatus() != null && !"Active".equalsIgnoreCase(employee.getStatus())) {
            throw new BadRequestException("Employee is not active.");
        }
        return employee;
    }

    private Attendance getOrCreateToday(String employeeId, Shift shift) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndDate(employeeId, today);
        if (existing.isPresent()) {
            return existing.get();
        }
        return Attendance.builder()
                .employeeId(employeeId)
                .date(today)
                .shiftId(shift.getId())
                .sessions(new ArrayList<>())
                .breaks(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void writeAudit(String employeeId, String attendanceId, String action, String ip, String ua,
                             String prev, String next, String reason, String performedBy) {
        auditLogRepository.save(AttendanceAuditLog.builder()
                .employeeId(employeeId)
                .attendanceId(attendanceId)
                .action(action)
                .timestamp(LocalDateTime.now())
                .ipAddress(ip)
                .userAgent(ua)
                .previousValue(prev)
                .newValue(next)
                .reason(reason)
                .performedBy(performedBy)
                .build());
    }

    // ---------- check-in / check-out / breaks ----------

    @Override
    public Attendance checkIn(String employeeId, String ipAddress, String userAgent) {
        requireActiveEmployee(employeeId);
        Shift shift = resolveShift(employeeId);
        Attendance attendance = getOrCreateToday(employeeId, shift);
        // TODO: if resolveDayType() returns WEEKLY_OFF and WeekendWorkPolicy.requiresManagerApproval
        // (or requiresHrApproval) is set, this is where a pre-approval gate would be enforced.
        // Per spec, weekend check-in is never blocked - it proceeds and is calculated normally.
        ensureDayType(attendance);

        boolean hasOpenSession = attendance.getSessions().stream().anyMatch(s -> s.getCheckOut() == null);
        if (hasOpenSession) {
            throw new BadRequestException("Already checked in! Please check out first.");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isFirstSessionOfDay = attendance.getSessions().isEmpty();
        attendance.getSessions().add(AttendanceSession.builder().checkIn(now).build());

        if (isFirstSessionOfDay && shift.getStartTime() != null) {
            long minutesLate = Duration.between(shift.getStartTime(), now.toLocalTime()).toMinutes();
            int grace = shift.getGracePeriodMinutes() != null ? shift.getGracePeriodMinutes() : 0;
            long lateMinutes = Math.max(0, minutesLate - grace);
            attendance.setLateMinutes(lateMinutes);
            attendance.setStatus(lateMinutes > 0 ? "Late" : "Working");
        } else if (attendance.getStatus() == null) {
            attendance.setStatus("Working");
        }
        attendance.setUpdatedAt(now);

        Attendance saved = attendanceRepository.save(attendance);
        writeAudit(employeeId, saved.getId(), "CHECK_IN", ipAddress, userAgent, null,
                now.toString(), null, employeeId);
        return saved;
    }

    @Override
    public Attendance checkOut(String employeeId, String ipAddress, String userAgent) {
        requireActiveEmployee(employeeId);
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new BadRequestException("Not checked in"));

        AttendanceSession openSession = attendance.getSessions().stream()
                .filter(s -> s.getCheckOut() == null)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Not checked in"));

        boolean hasOpenBreak = attendance.getBreaks().stream().anyMatch(b -> b.getEndTime() == null);
        if (hasOpenBreak) {
            throw new BadRequestException("End your break before checking out");
        }

        LocalDateTime now = LocalDateTime.now();
        String prev = openSession.getCheckIn() + " -> open";
        openSession.setCheckOut(now);

        Shift shift = resolveShift(employeeId);
        recomputeAggregates(attendance, shift);
        attendance.setUpdatedAt(now);

        Attendance saved = attendanceRepository.save(attendance);
        writeAudit(employeeId, saved.getId(), "CHECK_OUT", ipAddress, userAgent, prev,
                openSession.getCheckIn() + " -> " + now, null, employeeId);
        return saved;
    }

    @Override
    public Attendance startBreak(String employeeId, String ipAddress, String userAgent) {
        requireActiveEmployee(employeeId);
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new BadRequestException("Not checked in"));

        boolean hasOpenSession = attendance.getSessions().stream().anyMatch(s -> s.getCheckOut() == null);
        if (!hasOpenSession) {
            throw new BadRequestException("Not checked in");
        }
        boolean hasOpenBreak = attendance.getBreaks().stream().anyMatch(b -> b.getEndTime() == null);
        if (hasOpenBreak) {
            throw new BadRequestException("Break already in progress");
        }

        LocalDateTime now = LocalDateTime.now();
        attendance.getBreaks().add(AttendanceBreak.builder().startTime(now).build());
        attendance.setStatus("OnBreak");
        attendance.setUpdatedAt(now);

        Attendance saved = attendanceRepository.save(attendance);
        writeAudit(employeeId, saved.getId(), "BREAK_START", ipAddress, userAgent, null, now.toString(), null, employeeId);
        return saved;
    }

    @Override
    public Attendance endBreak(String employeeId, String ipAddress, String userAgent) {
        requireActiveEmployee(employeeId);
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new BadRequestException("Not checked in"));

        AttendanceBreak openBreak = attendance.getBreaks().stream()
                .filter(b -> b.getEndTime() == null)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No break in progress"));

        LocalDateTime now = LocalDateTime.now();
        String prev = openBreak.getStartTime() + " -> open";
        openBreak.setEndTime(now);
        attendance.setStatus("Working");
        attendance.setUpdatedAt(now);

        Attendance saved = attendanceRepository.save(attendance);
        writeAudit(employeeId, saved.getId(), "BREAK_END", ipAddress, userAgent, prev,
                openBreak.getStartTime() + " -> " + now, null, employeeId);
        return saved;
    }

    /**
     * Shared aggregate recomputation used by checkOut() and by
     * RegularizationServiceImpl when a correction is approved.
     *
     * Status priority (documented per task spec):
     *   1. MissingCheckIn - no sessions at all but checkout was attempted (defensive; not normally reachable)
     *   2. Absent - no sessions and nothing else to compute
     *   3. HalfDay - effectiveWorkingMinutes < shift.halfDayThresholdMinutes
     *   4. ShortHours - effectiveWorkingMinutes < shift.requiredWorkingMinutes
     *   5. Present - effectiveWorkingMinutes >= shift.requiredWorkingMinutes
     * lateMinutes / earlyCheckoutMinutes / overtimeMinutes are separate numeric
     * flags layered on top of the dominant status word above.
     */
    @Override
    public void recomputeAggregates(Attendance attendance, Shift shift) {
        List<AttendanceSession> sessions = attendance.getSessions() != null ? attendance.getSessions() : new ArrayList<>();
        List<AttendanceBreak> breaks = attendance.getBreaks() != null ? attendance.getBreaks() : new ArrayList<>();

        long totalDuration = 0;
        LocalDateTime lastCheckOut = null;
        for (AttendanceSession s : sessions) {
            if (s.getCheckIn() != null && s.getCheckOut() != null) {
                totalDuration += Duration.between(s.getCheckIn(), s.getCheckOut()).toMinutes();
                if (lastCheckOut == null || s.getCheckOut().isAfter(lastCheckOut)) {
                    lastCheckOut = s.getCheckOut();
                }
            }
        }

        long totalBreak = 0;
        for (AttendanceBreak b : breaks) {
            if (b.getStartTime() != null && b.getEndTime() != null) {
                totalBreak += Duration.between(b.getStartTime(), b.getEndTime()).toMinutes();
            }
        }

        long effective = Math.max(0, totalDuration - totalBreak);
        int overtimeThreshold = shift.getOvertimeThresholdMinutes() != null ? shift.getOvertimeThresholdMinutes() : 480;
        int requiredMinutes = shift.getRequiredWorkingMinutes() != null ? shift.getRequiredWorkingMinutes() : 480;
        int halfDayThreshold = shift.getHalfDayThresholdMinutes() != null ? shift.getHalfDayThresholdMinutes() : 240;
        long overtime = Math.max(0, effective - overtimeThreshold);

        long earlyCheckout = 0;
        if (lastCheckOut != null && shift.getEndTime() != null) {
            long minutesEarly = Duration.between(lastCheckOut.toLocalTime(), shift.getEndTime()).toMinutes();
            int earlyThreshold = shift.getEarlyCheckoutThresholdMinutes() != null ? shift.getEarlyCheckoutThresholdMinutes() : 0;
            earlyCheckout = Math.max(0, minutesEarly - earlyThreshold);
        }

        String status;
        if (sessions.isEmpty()) {
            status = "Absent";
        } else if (effective < halfDayThreshold) {
            status = "HalfDay";
        } else if (effective < requiredMinutes) {
            status = "ShortHours";
        } else {
            status = "Present";
        }

        // Weekend-work handling: if this day resolves to WEEKLY_OFF (weekend turned into a
        // working day) and the employee actually worked, apply WeekendWorkPolicy.treatAs
        // instead of the normal weekday requirement comparison, so the day is never
        // penalised (e.g. "ShortHours") for missing a weekday quota that doesn't apply to it.
        String dayType = ensureDayType(attendance);
        if ("WEEKLY_OFF".equals(dayType) && effective > 0) {
            WeekendWorkPolicy policy = workCalendarService.getWeekendWorkPolicy();
            String treatAs = policy.getTreatAs() != null ? policy.getTreatAs() : "OVERTIME";
            switch (treatAs) {
                case "NORMAL_HOURS":
                    // Treat like a normal working day - keep the status computed above as-is.
                    break;
                case "COMPENSATORY_OFF":
                    overtime = effective;
                    status = "Overtime";
                    String remarks = attendance.getRemarks();
                    String note = "Compensatory off eligible";
                    if (remarks == null || remarks.isBlank()) {
                        attendance.setRemarks(note);
                    } else if (!remarks.contains(note)) {
                        attendance.setRemarks(remarks + " (" + note + ")");
                    }
                    // NOTE: comp-off ledger crediting (CompOffController/CompOffRequest) is
                    // out of scope here - this only leaves a remarks note per spec.
                    break;
                case "OVERTIME":
                default:
                    overtime = effective;
                    status = "Overtime";
                    break;
            }
        }

        attendance.setTotalDurationMinutes(totalDuration);
        attendance.setTotalBreakMinutes(totalBreak);
        attendance.setEffectiveWorkingMinutes(effective);
        attendance.setOvertimeMinutes(overtime);
        attendance.setEarlyCheckoutMinutes(earlyCheckout);
        if (attendance.getLateMinutes() == null) {
            attendance.setLateMinutes(0L);
        }
        attendance.setStatus(status);
    }

    // ---------- read endpoints ----------

    @Override
    public AttendanceStatusDto getTodayStatus(String employeeId) {
        Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndDate(employeeId, LocalDate.now());
        Shift shift = resolveShift(employeeId);
        int required = shift.getRequiredWorkingMinutes() != null ? shift.getRequiredWorkingMinutes() : 480;

        if (existing.isEmpty()) {
            return AttendanceStatusDto.builder()
                    .attendance(null)
                    .currentStatus("NotCheckedIn")
                    .checkedIn(false)
                    .onBreak(false)
                    .workingMinutesSoFar(0)
                    .breakMinutesSoFar(0)
                    .requiredWorkingMinutes(required)
                    .remainingMinutes(required)
                    .build();
        }

        Attendance attendance = existing.get();
        LocalDateTime now = LocalDateTime.now();

        boolean onBreak = attendance.getBreaks().stream().anyMatch(b -> b.getEndTime() == null);
        boolean checkedIn = attendance.getSessions().stream().anyMatch(s -> s.getCheckOut() == null);

        long workingMinutes = 0;
        for (AttendanceSession s : attendance.getSessions()) {
            if (s.getCheckIn() != null) {
                LocalDateTime end = s.getCheckOut() != null ? s.getCheckOut() : now;
                workingMinutes += Duration.between(s.getCheckIn(), end).toMinutes();
            }
        }
        long breakMinutes = 0;
        for (AttendanceBreak b : attendance.getBreaks()) {
            if (b.getStartTime() != null) {
                LocalDateTime end = b.getEndTime() != null ? b.getEndTime() : now;
                breakMinutes += Duration.between(b.getStartTime(), end).toMinutes();
            }
        }
        long effectiveSoFar = Math.max(0, workingMinutes - breakMinutes);

        String currentStatus;
        if (onBreak) {
            currentStatus = "OnBreak";
        } else if (checkedIn) {
            currentStatus = "Working";
        } else if (!attendance.getSessions().isEmpty()) {
            currentStatus = "CheckedOut";
        } else {
            currentStatus = "NotCheckedIn";
        }

        return AttendanceStatusDto.builder()
                .attendance(attendance)
                .currentStatus(currentStatus)
                .checkedIn(checkedIn)
                .onBreak(onBreak)
                .workingMinutesSoFar(effectiveSoFar)
                .breakMinutesSoFar(breakMinutes)
                .requiredWorkingMinutes(required)
                .remainingMinutes(Math.max(0, required - effectiveSoFar))
                .build();
    }

    @Override
    public Attendance getTodayAttendance(String employeeId) {
        return attendanceRepository.findByEmployeeIdAndDate(employeeId, LocalDate.now()).orElse(null);
    }

    @Override
    public List<Attendance> getHistory(String employeeId, LocalDate from, LocalDate to) {
        return attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, from, to);
    }

    @Override
    public Attendance getAttendanceForDate(String employeeId, LocalDate date) {
        return attendanceRepository.findByEmployeeIdAndDate(employeeId, date).orElse(null);
    }

    @Override
    public List<Attendance> getMonthlyAttendance(String employeeId, int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, start, end);
    }

    @Override
    public MonthlyAttendanceSummaryDto getMonthlySummary(String employeeId, int month, int year) {
        List<Attendance> records = getMonthlyAttendance(employeeId, month, year);

        long present = records.stream().filter(a -> "Present".equalsIgnoreCase(a.getStatus())
                || "FullDay".equalsIgnoreCase(a.getStatus())).count();
        long absent = records.stream().filter(a -> "Absent".equalsIgnoreCase(a.getStatus())).count();
        long lateCount = records.stream().filter(a -> a.getLateMinutes() != null && a.getLateMinutes() > 0).count();
        long earlyCount = records.stream().filter(a -> a.getEarlyCheckoutMinutes() != null && a.getEarlyCheckoutMinutes() > 0).count();
        long totalOvertime = records.stream().mapToLong(a -> a.getOvertimeMinutes() != null ? a.getOvertimeMinutes() : 0).sum();
        double avgEffective = records.stream()
                .mapToLong(a -> a.getEffectiveWorkingMinutes() != null ? a.getEffectiveWorkingMinutes() : 0)
                .average().orElse(0);

        return MonthlyAttendanceSummaryDto.builder()
                .employeeId(employeeId)
                .month(month)
                .year(year)
                .workingDays(records.size())
                .present(present)
                .absent(absent)
                .leave(0)
                .lateCount(lateCount)
                .earlyCheckoutCount(earlyCount)
                .totalOvertimeMinutes(totalOvertime)
                .averageEffectiveWorkingMinutes(avgEffective)
                .build();
    }

    @Override
    public List<TeamAttendanceDto> getTeamAttendance(String managerId, LocalDate date) {
        List<Employee> reports = employeeRepository.findByManagerId(managerId);
        List<TeamAttendanceDto> result = new ArrayList<>();
        for (Employee emp : reports) {
            Optional<Attendance> attOpt = attendanceRepository.findByEmployeeIdAndDate(emp.getEmployeeId(), date);
            String status = attOpt.map(Attendance::getStatus).orElse("Absent");
            LocalDateTime checkIn = attOpt.flatMap(a -> a.getSessions().stream().findFirst())
                    .map(AttendanceSession::getCheckIn).orElse(null);
            LocalDateTime checkOut = attOpt.flatMap(a -> a.getSessions().stream()
                    .max(Comparator.comparing(s -> s.getCheckIn() != null ? s.getCheckIn() : LocalDateTime.MIN)))
                    .map(AttendanceSession::getCheckOut).orElse(null);
            Long workingMinutes = attOpt.map(Attendance::getEffectiveWorkingMinutes).orElse(0L);

            result.add(TeamAttendanceDto.builder()
                    .employeeId(emp.getEmployeeId())
                    .employeeName(emp.getFirstName() + " " + emp.getLastName())
                    .status(status)
                    .checkIn(checkIn)
                    .checkOut(checkOut)
                    .workingMinutes(workingMinutes)
                    .build());
        }
        return result;
    }

    @Override
    public List<Attendance> getAllAttendanceForDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }

    // ---------- HR monthly register grid ----------

    private static final Set<String> PRESENT_ISH_STATUSES = Set.of(
            "Present", "FullDay", "ShortHours", "HalfDay", "Overtime", "CheckedOut");

    @Override
    public List<MonthlyRegisterRowDto> getMonthlyRegister(int month, int year, String departmentId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        List<Employee> employees = employeeRepository.findAllActive().stream()
                .filter(e -> e.getStatus() == null || "Active".equalsIgnoreCase(e.getStatus()))
                .filter(e -> departmentId == null || departmentId.isBlank() || departmentId.equals(e.getDepartmentId()))
                .collect(Collectors.toList());

        List<MonthlyRegisterRowDto> rows = new ArrayList<>();
        for (Employee emp : employees) {
            String employeeId = emp.getEmployeeId();

            List<Attendance> monthAttendance = attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, start, end);
            Map<LocalDate, Attendance> attendanceByDate = new HashMap<>();
            for (Attendance a : monthAttendance) {
                attendanceByDate.put(a.getDate(), a);
            }

            Map<String, String> days = new LinkedHashMap<>();
            long presentCount = 0;

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = yearMonth.atDay(day);
                String letter;

                // Hierarchical Work Calendar resolver replaces the old direct
                // shift.weeklyOffDays lookup - see WorkCalendarService.resolveDayType().
                String dayType = workCalendarService.resolveDayType(employeeId, date).getDayType();

                if ("LEAVE".equals(dayType)) {
                    letter = "L";
                } else if ("PUBLIC_HOLIDAY".equals(dayType)) {
                    letter = "H";
                } else if ("WEEKLY_OFF".equals(dayType)) {
                    letter = "·"; // weekly off dot
                } else {
                    // WORKING / SPECIAL_WORKING_DAY - fall through to existing P/A logic unchanged.
                    Attendance att = attendanceByDate.get(date);
                    boolean present = att != null && (
                            (att.getEffectiveWorkingMinutes() != null && att.getEffectiveWorkingMinutes() > 0)
                                    || (att.getStatus() != null && PRESENT_ISH_STATUSES.contains(att.getStatus())));
                    if (present) {
                        letter = "P";
                    } else if (date.isAfter(today)) {
                        letter = "";
                    } else {
                        letter = "A";
                    }
                }

                if ("P".equals(letter)) {
                    presentCount++;
                }
                days.put(String.valueOf(day), letter);
            }

            rows.add(MonthlyRegisterRowDto.builder()
                    .employeeId(employeeId)
                    .employeeCode(employeeId)
                    .name(((emp.getFirstName() != null ? emp.getFirstName() : "") + " " +
                            (emp.getLastName() != null ? emp.getLastName() : "")).trim())
                    .days(days)
                    .presentCount(presentCount)
                    .build());
        }

        return rows;
    }

    private boolean isOnApprovedLeave(List<LeaveRequest> approvedLeaves, LocalDate date) {
        for (LeaveRequest lr : approvedLeaves) {
            if (lr.getStartDate() == null || lr.getEndDate() == null) {
                continue;
            }
            if (!date.isBefore(lr.getStartDate()) && !date.isAfter(lr.getEndDate())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void resetDailyAttendance() {
        LocalDate today = LocalDate.now();
        List<Attendance> priorDayDocs = attendanceRepository.findByDateBefore(today);
        for (Attendance attendance : priorDayDocs) {
            boolean hasOpenSession = attendance.getSessions().stream().anyMatch(s -> s.getCheckOut() == null);
            if (!hasOpenSession) {
                continue;
            }
            Shift shift = resolveShift(attendance.getEmployeeId());
            LocalTime shiftEnd = shift.getEndTime() != null ? shift.getEndTime() : LocalTime.of(18, 0);
            LocalDateTime autoCloseTime = LocalDateTime.of(attendance.getDate(), shiftEnd);

            for (AttendanceSession s : attendance.getSessions()) {
                if (s.getCheckOut() == null) {
                    String prev = s.getCheckIn() + " -> open";
                    s.setCheckOut(autoCloseTime);
                    writeAudit(attendance.getEmployeeId(), attendance.getId(), "AUTO_CLOSE", null, null,
                            prev, s.getCheckIn() + " -> " + autoCloseTime, "System auto-close", "SYSTEM");
                }
            }
            // Close any dangling open break too, so aggregates aren't skewed.
            for (AttendanceBreak b : attendance.getBreaks()) {
                if (b.getEndTime() == null) {
                    b.setEndTime(autoCloseTime);
                }
            }

            recomputeAggregates(attendance, shift);
            String remarks = attendance.getRemarks();
            if (remarks == null || remarks.trim().isEmpty()) {
                attendance.setRemarks("System Auto Clock-out");
            } else if (!remarks.contains("System Auto Clock-out")) {
                attendance.setRemarks(remarks + " (System Auto Clock-out)");
            }
            attendance.setUpdatedAt(LocalDateTime.now());
            attendanceRepository.save(attendance);
        }
    }

    // ---------- shift CRUD ----------

    @Override
    public Shift createShift(Shift shift) {
        return shiftRepository.save(shift);
    }

    @Override
    public List<Shift> getShifts() {
        return shiftRepository.findAll();
    }

    @Override
    public Shift updateShift(String id, Shift shift) {
        Shift existing = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + id));
        shift.setId(existing.getId());
        return shiftRepository.save(shift);
    }
}
