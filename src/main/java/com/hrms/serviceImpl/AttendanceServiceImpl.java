package com.hrms.serviceImpl;

import com.hrms.entity.Attendance;
import com.hrms.exception.BadRequestException;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.AttendanceRepository;
import com.hrms.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Override
    public Attendance clockIn(String employeeId, String status, String remarks) {
        LocalDate today = LocalDate.now();
        List<Attendance> existing = attendanceRepository.findByEmployeeIdAndDate(employeeId, today);
        boolean alreadyClockedIn = existing.stream().anyMatch(a -> a.getClockOut() == null);
        if (alreadyClockedIn) {
            throw new BadRequestException("Already clocked in! Please clock out first.");
        }

        Attendance attendance = Attendance.builder()
                .employeeId(employeeId)
                .date(today)
                .clockIn(LocalTime.now())
                .status(status != null ? status : "Present")
                .remarks(remarks)
                .build();

        return attendanceRepository.save(attendance);
    }

    @Override
    public Attendance clockOut(String employeeId) {
        LocalDate today = LocalDate.now();
        List<Attendance> existing = attendanceRepository.findByEmployeeIdAndDate(employeeId, today);
        Attendance activeRecord = existing.stream()
                .filter(a -> a.getClockOut() == null)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No active clock-in record found for today. Please clock in first."));

        LocalTime outTime = LocalTime.now();
        activeRecord.setClockOut(outTime);

        // Sum up total minutes worked today
        long totalMinutes = 0;
        for (Attendance r : existing) {
            LocalTime in = r.getClockIn();
            LocalTime out = r.getClockOut();
            if (r.getId() != null && r.getId().equals(activeRecord.getId())) {
                out = outTime;
            }
            if (in != null && out != null) {
                java.time.Duration duration = java.time.Duration.between(in, out);
                totalMinutes += duration.toMinutes();
            }
        }

        // Min 7 hours (420 minutes) is Present, otherwise Absent
        String newStatus = totalMinutes >= 420 ? "Present" : "Absent";

        // Update all other records of today to have the same aggregated status so counts are consistent
        for (Attendance r : existing) {
            if (!newStatus.equals(r.getStatus())) {
                r.setStatus(newStatus);
                attendanceRepository.save(r);
            }
        }
        activeRecord.setStatus(newStatus);

        return attendanceRepository.save(activeRecord);
    }

    @Override
    public Attendance getTodayAttendance(String employeeId) {
        List<Attendance> existing = attendanceRepository.findByEmployeeIdAndDate(employeeId, LocalDate.now());
        if (existing.isEmpty()) {
            return null;
        }
        return existing.stream()
                .filter(a -> a.getClockOut() == null)
                .findFirst()
                .orElse(existing.get(existing.size() - 1));
    }

    @Override
    public List<Attendance> getMonthlyAttendance(String employeeId, int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, start, end);
    }

    @Override
    public List<Attendance> getAllAttendanceForDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }

    @Override
    public void resetDailyAttendance() {
        LocalDate today = LocalDate.now();
        List<Attendance> unclosedRecords = attendanceRepository.findByDateBeforeAndClockOutIsNull(today);
        for (Attendance record : unclosedRecords) {
            LocalTime outTime = LocalTime.of(18, 0);
            record.setClockOut(outTime); // set default clock out time to 18:00
            String currentRemarks = record.getRemarks();
            if (currentRemarks == null || currentRemarks.trim().isEmpty()) {
                record.setRemarks("System Auto Clock-out");
            } else if (!currentRemarks.contains("System Auto Clock-out")) {
                record.setRemarks(currentRemarks + " (System Auto Clock-out)");
            }
            
            // Recalculate status for this user on that date
            List<Attendance> dayRecords = attendanceRepository.findByEmployeeIdAndDate(record.getEmployeeId(), record.getDate());
            long totalMinutes = 0;
            for (Attendance r : dayRecords) {
                LocalTime in = r.getClockIn();
                LocalTime out = r.getClockOut();
                if (r.getId() != null && r.getId().equals(record.getId())) {
                    out = outTime;
                }
                if (in != null && out != null) {
                    java.time.Duration duration = java.time.Duration.between(in, out);
                    totalMinutes += duration.toMinutes();
                }
            }
            String newStatus = totalMinutes >= 420 ? "Present" : "Absent";
            for (Attendance r : dayRecords) {
                r.setStatus(newStatus);
                attendanceRepository.save(r);
            }
            record.setStatus(newStatus);
            attendanceRepository.save(record);
        }
    }
}
