package com.smarthr.backend.controller;

import com.smarthr.backend.model.Holiday;
import com.smarthr.backend.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@CrossOrigin(origins = "*")
public class HolidayController {

    @Autowired
    private HolidayRepository holidayRepo;

    @GetMapping("/all")
    public List<Holiday> getAllHolidays() {
        return holidayRepo.findAllByOrderByHolidayDateAsc();
    }

    @PostMapping("/add")
    public Holiday addHoliday(@RequestBody Holiday holiday) {
        return holidayRepo.save(holiday);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteHoliday(@PathVariable Long id) {
        holidayRepo.deleteById(id);
    }
}
