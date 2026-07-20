package com.hrms.serviceImpl;

import com.hrms.entity.Holiday;
import com.hrms.exception.ResourceNotFoundException;
import com.hrms.repository.HolidayRepository;
import com.hrms.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HolidayServiceImpl implements HolidayService {

    @Autowired
    private HolidayRepository holidayRepository;

    @Override
    public List<Holiday> getAllHolidays() {
        return holidayRepository.findAll();
    }

    @Override
    public Holiday createHoliday(Holiday holiday) {
        return holidayRepository.save(holiday);
    }

    @Override
    public void deleteHoliday(String id) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + id));
        holidayRepository.delete(holiday);
    }
}
