package com.smarthr.backend.controller;

import com.smarthr.backend.model.InterviewSlot;
import com.smarthr.backend.model.User;
import com.smarthr.backend.repository.InterviewSlotRepository;
import com.smarthr.backend.service.SchedulingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/scheduling")
@CrossOrigin(origins = "*")
public class SchedulingController {

    @Autowired
    private InterviewSlotRepository slotRepo;

    @Autowired
    private SchedulingService schedulingService;

    // Employee: Request a new interview slot (AI auto-matches it)
    @PostMapping("/request")
    public ResponseEntity<?> requestSlot(@RequestBody InterviewSlot slot, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null)
            return ResponseEntity.status(401).body("Unauthenticated.");

        slot.setRequestedById(user.getId());
        slot.setCandidateName(user.getFullName());
        slot.setCandidateEmail(user.getEmail());
        if (slot.getStatus() == null)
            slot.setStatus("Pending");

        // AI auto-match slot
        InterviewSlot matched = schedulingService.autoMatch(slot);
        return ResponseEntity.ok(slotRepo.save(matched));
    }

    // Employee/Admin: Preview AI slot match without saving
    @PostMapping("/preview-match")
    public ResponseEntity<?> previewMatch(@RequestBody InterviewSlot slot) {
        InterviewSlot preview = schedulingService.autoMatch(slot);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scheduledTime", preview.getScheduledTime());
        result.put("scheduledDate", preview.getScheduledDate());
        result.put("aiStatus", preview.getAiStatus());
        result.put("aiConfidenceScore", preview.getAiConfidenceScore());
        result.put("aiMatchReason", preview.getAiMatchReason());
        result.put("meetingLink", preview.getMeetingLink());
        result.put("alternatives", schedulingService.suggestAlternatives(slot));
        return ResponseEntity.ok(result);
    }

    // Employee: Get their own scheduled slots
    @GetMapping("/my-slots")
    public ResponseEntity<?> getMySlots(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null)
            return ResponseEntity.status(401).body("Unauthenticated.");
        return ResponseEntity.ok(slotRepo.findByRequestedByIdOrderByCreatedAtDesc(user.getId()));
    }

    // Admin: Get all interview slots
    @GetMapping("/all")
    public ResponseEntity<?> getAllSlots(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !"ADMIN".equals(user.getRole()))
            return ResponseEntity.status(403).body("Admins only.");
        return ResponseEntity.ok(slotRepo.findAllByOrderByCreatedAtDesc());
    }

    // Admin: Aggregated scheduling stats
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !"ADMIN".equals(user.getRole()))
            return ResponseEntity.status(403).body("Admins only.");

        List<InterviewSlot> all = slotRepo.findAll();
        long total = all.size();
        long confirmed = all.stream().filter(s -> "Confirmed".equalsIgnoreCase(s.getStatus())).count();
        long pending = all.stream().filter(s -> "Pending".equalsIgnoreCase(s.getStatus())).count();
        long cancelled = all.stream().filter(s -> "Cancelled".equalsIgnoreCase(s.getStatus())).count();
        long completed = all.stream().filter(s -> "Completed".equalsIgnoreCase(s.getStatus())).count();

        // Type breakdown
        Map<String, Long> byType = new LinkedHashMap<>();
        for (InterviewSlot s : all) {
            String t = s.getInterviewType() != null ? s.getInterviewType() : "Unknown";
            byType.put(t, byType.getOrDefault(t, 0L) + 1L);
        }

        // Avg AI confidence
        double avgConfidence = all.stream()
                .filter(s -> s.getAiConfidenceScore() != null)
                .mapToInt(InterviewSlot::getAiConfidenceScore)
                .average().orElse(0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("confirmed", confirmed);
        stats.put("pending", pending);
        stats.put("cancelled", cancelled);
        stats.put("completed", completed);
        stats.put("byType", byType);
        stats.put("avgAiConfidence", Math.round(avgConfidence));
        return ResponseEntity.ok(stats);
    }

    // Admin/Employee: Update slot status
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null)
            return ResponseEntity.status(401).body("Unauthenticated.");
        InterviewSlot slot = slotRepo.findById(id).orElseThrow();
        slot.setStatus(body.get("status"));
        return ResponseEntity.ok(slotRepo.save(slot));
    }

    // Admin: Delete a slot
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSlot(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !"ADMIN".equals(user.getRole()))
            return ResponseEntity.status(403).body("Admins only.");
        slotRepo.deleteById(id);
        return ResponseEntity.ok("Deleted.");
    }

    // AI Chatbot — role-aware
    @PostMapping("/ai-assistant")
    public ResponseEntity<?> aiAssistant(@RequestBody Map<String, String> payload, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null)
            return ResponseEntity.status(401).body("Unauthenticated.");
        String message = payload.get("message");
        if (message == null || message.trim().isEmpty())
            return ResponseEntity.badRequest().body("Empty message.");
        String response = schedulingService.getChatbotResponse(message, user.getRole());
        return ResponseEntity.ok(Collections.singletonMap("response", response));
    }
}
