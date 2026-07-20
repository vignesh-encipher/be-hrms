package com.hrms.service;

import com.hrms.entity.Holiday;
import java.util.List;

public interface HolidayService {
    List<Holiday> getAllHolidays();
    Holiday createHoliday(Holiday holiday);
    void deleteHoliday(String id);
}
