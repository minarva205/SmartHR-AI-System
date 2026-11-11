package com.smarthr.backend.controller;

import com.smarthr.backend.model.Feedback;
import com.smarthr.backend.model.User;
import com.smarthr.backend.repository.FeedbackRepository;
import com.smarthr.backend.service.SentimentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*")
public class FeedbackController {

    @Autowired
    private FeedbackRepository feedbackRepo;

    @Autowired
    private SentimentService sentimentService;

    // Submit new feedback
    @PostMapping("/submit")
    public Feedback submitFeedback(@RequestBody Feedback feedback) {
        // Run AI Sentiment scan on the feedback message before saving
        Feedback enriched = sentimentService.analyzeFeedback(feedback);
        return feedbackRepo.save(enriched);
    }

    // Real-time dynamic analysis without saving
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeFeedbackPreview(@RequestBody Map<String, String> body) {
        String message = body.get("message");
        String category = body.get("category");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message is empty.");
        }
        Feedback temp = new Feedback();
        temp.setMessage(message);
        temp.setCategory(category != null ? category : "Suggestion");
        Feedback analyzed = sentimentService.analyzeFeedback(temp);
        return ResponseEntity.ok(analyzed);
    }

    // Get organizational feedback metrics for HR Admin
    @GetMapping("/stats")
    public ResponseEntity<?> getSentimentStats(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(403).body("Access denied. Admins only.");
        }

        List<Feedback> all = feedbackRepo.findAll();
        long total = all.size();
        long resolved = all.stream().filter(f -> "Resolved".equalsIgnoreCase(f.getStatus())).count();
        long pending = all.stream().filter(f -> "Pending".equalsIgnoreCase(f.getStatus()) || f.getStatus() == null).count();

        double moraleIndex = 50.0;
        if (total > 0) {
            moraleIndex = all.stream()
                .mapToDouble(f -> f.getSentimentScore() != null ? f.getSentimentScore() : 50.0)
                .average()
                .orElse(50.0);
        }

        long positive = all.stream().filter(f -> "Positive".equalsIgnoreCase(f.getSentiment())).count();
        long negative = all.stream().filter(f -> "Negative".equalsIgnoreCase(f.getSentiment())).count();
        long neutral = all.stream().filter(f -> "Neutral".equalsIgnoreCase(f.getSentiment()) || f.getSentiment() == null).count();

        Map<String, Long> tones = new HashMap<>();
        for (Feedback f : all) {
            String tone = f.getEmotionalTone() != null ? f.getEmotionalTone() : "Neutral";
            tones.put(tone, tones.getOrDefault(tone, 0L) + 1);
        }

        Map<String, Long> categories = new HashMap<>();
        for (Feedback f : all) {
            String cat = f.getCategory() != null ? f.getCategory() : "Other";
            categories.put(cat, categories.getOrDefault(cat, 0L) + 1);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("resolved", resolved);
        stats.put("pending", pending);
        stats.put("moraleIndex", Math.round(moraleIndex * 10.0) / 10.0);
        stats.put("positive", positive);
        stats.put("negative", negative);
        stats.put("neutral", neutral);
        stats.put("tones", tones);
        stats.put("categories", categories);

        return ResponseEntity.ok(stats);
    }

    // Empathetic Counselor / Strategic advisor chatbot
    @PostMapping("/ai-assistant")
    public ResponseEntity<?> aiAssistant(@RequestBody Map<String, String> payload, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthenticated session.");
        }

        String userMessage = payload.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message is empty.");
        }

        String aiResponse = "";
        String cleanMessage = userMessage.toLowerCase().trim();

        if ("ADMIN".equals(user.getRole())) {
            // HR Strategic Advisor Chat Assistant
            if (cleanMessage.contains("lowest morale") || cleanMessage.contains("who is unhappy") || cleanMessage.contains("negative") || cleanMessage.contains("worst")) {
                List<Feedback> unhappy = feedbackRepo.findAll().stream()
                    .filter(f -> "Negative".equalsIgnoreCase(f.getSentiment()))
                    .collect(Collectors.toList());
                if (unhappy.isEmpty()) {
                    aiResponse = "Excellent! There are **no feedbacks with Negative sentiment** in the database right now. Workforce morale index is highly optimistic.";
                } else {
                    StringBuilder sb = new StringBuilder("Here is a summary of workforce logs flagged with **Negative Sentiment**:\n\n");
                    for (Feedback f : unhappy) {
                        sb.append("- **Subject**: \"").append(f.getSubject()).append("\"\n");
                        sb.append("  *Emotion*: ").append(f.getEmotionalTone()).append(" (Morale: ").append(Math.round(f.getSentimentScore())).append("%)\n");
                        sb.append("  *Grievance*: ").append(f.getMessage()).append("\n\n");
                    }
                    sb.append("I suggest setting up structured career syncs and workload reviews for these departments.");
                    aiResponse = sb.toString();
                }
            } else if (cleanMessage.contains("hybrid") || cleanMessage.contains("remote")) {
                aiResponse = "Recent surveys show strong suggestions regarding **hybrid work policies**. Employees are suggesting a hybrid structure of 3 days home, 2 days office to reduce commute exhaustion.\n\n" +
                        "**Strategic Recommendations**:\n" +
                        "- Conduct a team evaluation study of remote performance ratios.\n" +
                        "- Introduce flexible check-in parameters to offset travel bottlenecks.";
            } else if (cleanMessage.contains("stress") || cleanMessage.contains("burnout") || cleanMessage.contains("overwork")) {
                aiResponse = "Overload indicators have been detected in **Grievance logs**.\n\n" +
                        "**Action checklist suggested by SentimentBot AI**:\n" +
                        "1. Cap team overtime hours immediately.\n" +
                        "2. Redistribute project deliverables to reduce overload.\n" +
                        "3. Encourage teams to use accumulated wellness leaves.";
            } else {
                aiResponse = "Hello! I am **SentimentBot AI**, your strategic organizational psychology advisor. I can analyze company-wide feedback morale trends, pinpoint structural stress areas, summarize employee grievances, and help draft response letters. Try asking me: *'Which logs show negative sentiment?'* or *'Summarize thoughts about hybrid work.'*";
            }
        } else {
            // Employee Wellness counselor
            if (cleanMessage.contains("stress") || cleanMessage.contains("burnt") || cleanMessage.contains("exhausted") || cleanMessage.contains("overworked")) {
                aiResponse = "I hear you, and your wellness is incredibly valuable. Work stress and burnout can be deeply challenging. Here are some actionable suggestions:\n\n" +
                        "- **Communicate Boundaries**: Try discussing task limits with your manager: *'I want to produce top-tier work, but my current task volume is exceeding standard limits. Can we align on priorities?'*\n" +
                        "- **Take Micro-Breaks**: Adopt a healthy schedule, stepping away from the screen for 5 minutes every hour.\n" +
                        "- **Wellness survey**: Write these stress factors in detail in our Feedback form on this page. All submissions can be sent **anonymously** to let HR adapt workloads.";
            } else if (cleanMessage.contains("underpaid") || cleanMessage.contains("salary") || cleanMessage.contains("low pay") || cleanMessage.contains("raise")) {
                aiResponse = "Compensation concerns can be quite stressful. If you feel underpaid compared to standards, here is how you can approach it constructively:\n\n" +
                        "- **Prepare Data**: Note down your key achievements, deliverables completed, and average market standards for your job profile.\n" +
                        "- **Sync with HR/Manager**: Propose an appraisal sync: *'I would love to outline my contributions over the past year and explore how we can align my compensation with current market averages.'*\n" +
                        "- **LMS Skill-up**: Taking certified skill courses in our **SmartHR LMS** helps demonstrate strong career growth initiative.";
            } else if (cleanMessage.contains("hello") || cleanMessage.contains("hi") || cleanMessage.contains("help")) {
                aiResponse = "Hello! I am **SentimentBot AI**, your confidential empathetic wellness assistant. I am here to support you in managing stress, resolving workplace bottlenecks, improving work-life balance, and drafting clear communications for your team. How can I help support you today?";
            } else {
                aiResponse = "Thank you for sharing this. Remember, your physical and mental well-being is vital. You can take short steps to practice wellness, manage timelines, or utilize our smart self-service channels. Feel free to ask me for advice on dealing with workload, remote productivity, or professional career growth!";
            }
        }

        return ResponseEntity.ok(Collections.singletonMap("response", aiResponse));
    }

    // Get feedbacks for a specific employee
    @GetMapping("/my-feedbacks/{empId}")
    public List<Feedback> getMyFeedbacks(@PathVariable Long empId) {
        return feedbackRepo.findByEmployeeIdOrderBySubmittedAtDesc(empId);
    }

    // Get all feedbacks for HR
    @GetMapping("/all")
    public List<Feedback> getAllFeedbacks() {
        return feedbackRepo.findAllByOrderBySubmittedAtDesc();
    }

    // Update feedback status
    @PutMapping("/{id}/status")
    public Feedback updateStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        Feedback fb = feedbackRepo.findById(id).orElseThrow();
        fb.setStatus(body.get("status"));
        return feedbackRepo.save(fb);
    }

    // Delete feedback
    @DeleteMapping("/{id}")
    public void deleteFeedback(@PathVariable Long id) {
        feedbackRepo.deleteById(id);
    }
}
