package com.smarthr.backend.controller;

import com.smarthr.backend.model.Event;
import com.smarthr.backend.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    @Autowired
    private EventRepository eventRepo;

    @GetMapping("/upcoming")
    public List<Event> getUpcomingEvents() {
        return eventRepo.findByEventDateGreaterThanEqualOrderByEventDateAsc(LocalDate.now().toString());
    }

    @PostMapping("/add")
    public Event addEvent(@RequestBody Event event) {
        return eventRepo.save(event);
    }

    @DeleteMapping("/{id}")
    public org.springframework.http.ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        if (eventRepo.existsById(id)) {
            eventRepo.deleteById(id);
            return org.springframework.http.ResponseEntity.ok().build();
        }
        return org.springframework.http.ResponseEntity.notFound().build();
    }
}