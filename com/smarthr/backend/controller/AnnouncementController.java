
package com.smarthr.backend.controller;

import com.smarthr.backend.model.Announcement;
import com.smarthr.backend.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin(origins = "*")
public class AnnouncementController {

    @Autowired
    private AnnouncementRepository announcementRepo;

    @GetMapping("/all")
    public List<Announcement> getAllAnnouncements() {
        return announcementRepo.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/add")
    public Announcement createAnnouncement(@RequestBody Announcement announcement) {
        return announcementRepo.save(announcement);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteAnnouncement(@PathVariable Long id) {
        announcementRepo.deleteById(id);
    }
}