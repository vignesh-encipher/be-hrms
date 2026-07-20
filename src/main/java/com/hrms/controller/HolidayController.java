package com.hrms.controller;

import com.hrms.entity.Holiday;
import com.hrms.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/holidays")
public class HolidayController {

    @Autowired
    private HolidayService holidayService;

    @GetMapping
    public ResponseEntity<List<Holiday>> getAllHolidays() {
        return ResponseEntity.ok(holidayService.getAllHolidays());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<Holiday> createHoliday(@RequestBody Holiday holiday) {
        return ResponseEntity.ok(holidayService.createHoliday(holiday));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('HR')")
    public ResponseEntity<Void> deleteHoliday(@PathVariable String id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }
}
