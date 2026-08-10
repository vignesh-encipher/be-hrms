package com.hrms.service;

import com.hrms.dto.CalendarDayDto;
import com.hrms.dto.DayTypeResultDto;
import com.hrms.entity.SpecialDateOverride;
import com.hrms.entity.WeekendWorkPolicy;
import com.hrms.entity.WorkCalendarAuditLog;
import com.hrms.entity.WorkCalendarRule;

import java.time.LocalDate;
import java.util.List;

public interface WorkCalendarService {

    // Core hierarchical resolver used by attendance calculation and calendar views.
    DayTypeResultDto resolveDayType(String employeeId, LocalDate date);

    WorkCalendarRule getEffectiveWeeklyPattern(String scope, String scopeRefId);

    WorkCalendarRule upsertWorkCalendarRule(WorkCalendarRule rule, String actorId);

    List<WorkCalendarRule> getRules(String scope, String scopeRefId, boolean includeHistory);

    SpecialDateOverride createSpecialDateOverride(SpecialDateOverride override, String actorId);

    List<SpecialDateOverride> listSpecialDateOverrides(String scope, String scopeRefId, LocalDate from, LocalDate to);

    WeekendWorkPolicy getWeekendWorkPolicy();

    WeekendWorkPolicy upsertWeekendWorkPolicy(WeekendWorkPolicy policy, String actorId);

    List<CalendarDayDto> getCalendarMonth(String scope, String scopeRefId, int month, int year);

    List<WorkCalendarAuditLog> getHistory(String scope, String scopeRefId);
}
