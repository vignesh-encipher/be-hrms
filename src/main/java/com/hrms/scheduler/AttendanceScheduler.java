package com.hrms.scheduler;

import com.hrms.service.AttendanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AttendanceScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceScheduler.class);

    @Autowired
    private AttendanceService attendanceService;

    // Run at midnight every day
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyAttendanceTask() {
        logger.info("Starting daily attendance reset task...");
        try {
            attendanceService.resetDailyAttendance();
            logger.info("Daily attendance reset completed successfully.");
        } catch (Exception e) {
            logger.error("Error occurred while resetting daily attendance: ", e);
        }
    }
}
