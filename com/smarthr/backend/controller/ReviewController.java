package com.smarthr.backend.controller;


import com.smarthr.backend.model.Review;
import com.smarthr.backend.model.User;

import com.smarthr.backend.repository.ReviewRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;



    // Helper: Verify admin session
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        return user != null && "ADMIN".equals(user.getRole());
    }

    // ──────────────────────────────────────────────
    // EXISTING CRUD ENDPOINTS
    // ──────────────────────────────────────────────

    @GetMapping("/all")
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    @GetMapping("/employee/{employeeId}")
    public List<Review> getReviewsByEmployeeId(@PathVariable Long employeeId) {
        return reviewRepository.findByEmployeeId(employeeId);
    }

    @PostMapping("/submit")
    public Review submitReview(@RequestBody Review review) {
        double avg = (review.getTechnicalRating() + review.getSoftSkillsRating() + review.getAttendanceRating()) / 3.0;
        review.setOverallScore(Math.round(avg * 10.0) / 10.0);
        return reviewRepository.save(review);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteReview(@PathVariable Long id) {
        reviewRepository.deleteById(id);
    }

    // ──────────────────────────────────────────────
    // AI FEATURE 1: Auto-Generate Manager Feedback
    // POST /api/reviews/ai-generate-feedback
    // ──────────────────────────────────────────────
    @PostMapping("/ai-generate-feedback")
    public ResponseEntity<?> autoGenerateFeedback(@RequestBody Map<String, Object> payload, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Admin access required.");
        }

        int tech = payload.get("technicalRating") != null ? ((Number) payload.get("technicalRating")).intValue() : 3;
        int soft = payload.get("softSkillsRating") != null ? ((Number) payload.get("softSkillsRating")).intValue() : 3;
        int attn = payload.get("attendanceRating") != null ? ((Number) payload.get("attendanceRating")).intValue() : 3;
        String empName = payload.get("employeeName") != null ? (String) payload.get("employeeName") : "The employee";

        double avg = (tech + soft + attn) / 3.0;
        StringBuilder sb = new StringBuilder();

        // Opening sentence based on overall performance
        if (avg >= 4.5) {
            sb.append(empName).append(" has delivered an outstanding performance this review cycle, consistently exceeding all expectations. ");
        } else if (avg >= 3.5) {
            sb.append(empName).append(" has performed commendably during this evaluation period, meeting most key performance targets with a positive trajectory. ");
        } else if (avg >= 2.5) {
            sb.append(empName).append(" has demonstrated a reasonable level of performance this period, with several areas that would benefit from focused improvement. ");
        } else {
            sb.append(empName).append(" has faced challenges this review cycle and requires structured support to realign with performance benchmarks. ");
        }

        // Technical commentary
        if (tech >= 5) {
            sb.append("Technical competence is exceptional — a strong command of domain tools and problem-solving methodology has been clearly evident in deliverables. ");
        } else if (tech >= 4) {
            sb.append("Technical skills are well above average, with consistent delivery of quality output and sound engineering judgment throughout the cycle. ");
        } else if (tech >= 3) {
            sb.append("Technical proficiency is at an acceptable standard, though deeper expertise and upskilling in core domain areas is recommended for the next cycle. ");
        } else {
            sb.append("Technical performance requires immediate improvement; enrollment in targeted training programs via the SmartHR LMS platform is strongly advised. ");
        }

        // Soft skills commentary
        if (soft >= 5) {
            sb.append("Interpersonal skills and team collaboration are exemplary — a positive and constructive force within cross-functional team dynamics. ");
        } else if (soft >= 4) {
            sb.append("Soft skills demonstrate strong communication capabilities and effective team synergy in daily workflows. ");
        } else if (soft >= 3) {
            sb.append("Collaboration and communication are satisfactory. Engaging in more structured team activities could further enhance interpersonal presence. ");
        } else {
            sb.append("Soft skills require focused development — a mentor-led communication program and guided team participation are strongly recommended. ");
        }

        // Attendance commentary
        if (attn >= 5) {
            sb.append("Attendance and punctuality have been impeccable throughout the cycle — a highly reliable and consistent professional. ");
        } else if (attn >= 4) {
            sb.append("Attendance record is excellent with minimal absences, demonstrating a strong professional commitment to time management. ");
        } else if (attn >= 3) {
            sb.append("Attendance levels are within acceptable norms, though maintaining higher punctuality consistency would benefit overall team coordination. ");
        } else {
            sb.append("Attendance patterns have been below acceptable thresholds and require an immediate corrective discussion and formal plan. ");
        }

        // Closing recommendation
        if (avg >= 4.0) {
            sb.append("Overall, ").append(empName).append(" is a highly valuable contributor and is recommended for performance-linked rewards and increased project responsibilities in the next review cycle.");
        } else if (avg >= 3.0) {
            sb.append("Overall, ").append(empName).append(" is encouraged to sustain current momentum and address flagged competency gaps through SmartHR LMS training programs to qualify for next-level advancement.");
        } else {
            sb.append("A structured 30-day Performance Improvement Plan (PIP) is recommended to help ").append(empName).append(" realign with team expectations and organizational KPIs in the coming cycle.");
        }

        return ResponseEntity.ok(Collections.singletonMap("feedback", sb.toString()));
    }

    // ──────────────────────────────────────────────
    // AI FEATURE 2: Per-Review AI Insight Report
    // GET /api/reviews/ai-insight/{id}
    // ──────────────────────────────────────────────
    @GetMapping("/ai-insight/{id}")
    public ResponseEntity<?> getAiInsight(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Admin access required.");
        }

        Optional<Review> reviewOpt = reviewRepository.findById(id);
        if (!reviewOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Review r = reviewOpt.get();
        String empName = r.getEmployee() != null ? r.getEmployee().getFullName() : "Employee";
        int tech = r.getTechnicalRating() != null ? r.getTechnicalRating() : 0;
        int soft = r.getSoftSkillsRating() != null ? r.getSoftSkillsRating() : 0;
        int attn = r.getAttendanceRating() != null ? r.getAttendanceRating() : 0;
        double overall = r.getOverallScore() != null ? r.getOverallScore() : 0.0;

        List<String> strengths = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        if (tech >= 4) strengths.add("Strong Technical Competence (" + tech + "/5)");
        else if (tech <= 2) { gaps.add("Technical Skills Deficit (" + tech + "/5)"); actions.add("Enroll in SmartHR LMS technical upskilling tracks immediately"); }
        else actions.add("Continue technical development with 1 LMS course per month");

        if (soft >= 4) strengths.add("Excellent Interpersonal & Collaboration Skills (" + soft + "/5)");
        else if (soft <= 2) { gaps.add("Soft Skills & Communication Gap (" + soft + "/5)"); actions.add("Assign mentor-led communication workshop within 30 days"); }
        else actions.add("Enroll in a structured team facilitation or public speaking course");

        if (attn >= 4) strengths.add("High Attendance Reliability & Punctuality (" + attn + "/5)");
        else if (attn <= 2) { gaps.add("Attendance & Punctuality Issues (" + attn + "/5)"); actions.add("Schedule confidential attendance review discussion with HR"); }

        if (overall >= 4.0 && actions.isEmpty()) {
            actions.add("Nominate for Senior/Lead role consideration in next cycle");
            actions.add("Assign as technical mentor to junior team members");
            actions.add("Include in high-visibility cross-functional strategic projects");
        } else if (overall < 3.0 && actions.isEmpty()) {
            actions.add("Initiate 30-day structured Performance Improvement Plan (PIP)");
            actions.add("Weekly 1-on-1 check-ins with direct manager for 60 days");
        }

        // Performance tier
        String tier, tierIcon;
        if (overall >= 4.5) { tier = "ELITE PERFORMER — Top 10% talent. Immediate succession planning recommended."; tierIcon = "🏆"; }
        else if (overall >= 3.5) { tier = "HIGH PERFORMER — Consistently above target. Strong growth trajectory."; tierIcon = "⭐"; }
        else if (overall >= 2.5) { tier = "DEVELOPING PERFORMER — On track with clear areas for structured improvement."; tierIcon = "📈"; }
        else { tier = "NEEDS SUPPORT — Initiate a structured Performance Improvement Plan (PIP) immediately."; tierIcon = "🔴"; }

        Map<String, Object> result = new HashMap<>();
        result.put("employeeName", empName);
        result.put("reviewDate", r.getReviewDate() != null ? r.getReviewDate().toString() : "Recent");
        result.put("overall", overall);
        result.put("tech", tech);
        result.put("soft", soft);
        result.put("attn", attn);
        result.put("strengths", strengths);
        result.put("gaps", gaps);
        result.put("actions", actions);
        result.put("tier", tier);
        result.put("tierIcon", tierIcon);

        return ResponseEntity.ok(result);
    }

    // ──────────────────────────────────────────────
    // AI FEATURE 3: Company-Wide AI Analytics
    // GET /api/reviews/ai-analytics
    // ──────────────────────────────────────────────
    @GetMapping("/ai-analytics")
    public ResponseEntity<?> getAiAnalytics(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Admin access required.");
        }

        List<Review> all = reviewRepository.findAll();

        long total = all.size();
        long elite = all.stream().filter(r -> r.getOverallScore() != null && r.getOverallScore() >= 4.5).count();
        long high = all.stream().filter(r -> r.getOverallScore() != null && r.getOverallScore() >= 3.5 && r.getOverallScore() < 4.5).count();
        long developing = all.stream().filter(r -> r.getOverallScore() != null && r.getOverallScore() >= 2.5 && r.getOverallScore() < 3.5).count();
        long needsSupport = all.stream().filter(r -> r.getOverallScore() != null && r.getOverallScore() < 2.5).count();

        double avgOverall = all.stream().filter(r -> r.getOverallScore() != null).mapToDouble(Review::getOverallScore).average().orElse(0.0);
        double avgTech = all.stream().filter(r -> r.getTechnicalRating() != null).mapToDouble(Review::getTechnicalRating).average().orElse(0.0);
        double avgSoft = all.stream().filter(r -> r.getSoftSkillsRating() != null).mapToDouble(Review::getSoftSkillsRating).average().orElse(0.0);
        double avgAttn = all.stream().filter(r -> r.getAttendanceRating() != null).mapToDouble(Review::getAttendanceRating).average().orElse(0.0);

        // Identify weakest skill area
        String weakestSkill = "Technical Skills";
        double minAvg = avgTech;
        if (avgSoft < minAvg) { weakestSkill = "Soft Skills & Collaboration"; minAvg = avgSoft; }
        if (avgAttn < minAvg) weakestSkill = "Attendance & Punctuality";

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("total", total);
        analytics.put("elite", elite);
        analytics.put("high", high);
        analytics.put("developing", developing);
        analytics.put("needsSupport", needsSupport);
        analytics.put("avgOverall", Math.round(avgOverall * 10.0) / 10.0);
        analytics.put("avgTech", Math.round(avgTech * 10.0) / 10.0);
        analytics.put("avgSoft", Math.round(avgSoft * 10.0) / 10.0);
        analytics.put("avgAttn", Math.round(avgAttn * 10.0) / 10.0);
        analytics.put("weakestSkill", weakestSkill);
        analytics.put("healthScore", (int) Math.round(avgOverall * 20.0));

        return ResponseEntity.ok(analytics);
    }

    // ──────────────────────────────────────────────
    // AI FEATURE 4: Employee AI Development Plan
    // GET /api/reviews/ai-development-plan/{employeeId}
    // ──────────────────────────────────────────────
    @GetMapping("/ai-development-plan/{employeeId}")
    public ResponseEntity<?> getDevelopmentPlan(@PathVariable Long employeeId, HttpSession session) {
        List<Review> reviews = reviewRepository.findByEmployeeId(employeeId);
        if (reviews.isEmpty()) {
            Map<String, String> empty = new HashMap<>();
            empty.put("plan", "No evaluation data found yet. Ask your manager to submit a performance review to unlock your personalized AI Development Plan.");
            empty.put("tier", "Pending");
            empty.put("tierIcon", "⏳");
            return ResponseEntity.ok(empty);
        }

        // Use the most recent review
        Review latest = reviews.stream()
            .filter(r -> r.getReviewDate() != null)
            .max(Comparator.comparing(Review::getReviewDate))
            .orElse(reviews.get(reviews.size() - 1));

        int tech = latest.getTechnicalRating() != null ? latest.getTechnicalRating() : 3;
        int soft = latest.getSoftSkillsRating() != null ? latest.getSoftSkillsRating() : 3;
        int attn = latest.getAttendanceRating() != null ? latest.getAttendanceRating() : 3;
        double overall = latest.getOverallScore() != null ? latest.getOverallScore() : 3.0;

        String tier, tierIcon;
        if (overall >= 4.5) { tier = "Elite Performer"; tierIcon = "🏆"; }
        else if (overall >= 3.5) { tier = "High Performer"; tierIcon = "⭐"; }
        else if (overall >= 2.5) { tier = "Developing Performer"; tierIcon = "📈"; }
        else { tier = "Needs Support"; tierIcon = "🎯"; }

        List<Map<String, String>> sections = new ArrayList<>();

        // Section 1: Current Standing
        Map<String, String> standing = new LinkedHashMap<>();
        standing.put("title", tierIcon + " Current Standing: " + tier);
        if (overall >= 4.5) {
            standing.put("body", "You are in the top 10% of organizational talent! Your performance is exceptional. Focus your energy on leadership, mentoring junior colleagues, and driving strategic initiatives that expand your organizational impact beyond your immediate team.");
        } else if (overall >= 3.5) {
            standing.put("body", "Excellent work! You are consistently above target. A few targeted improvements can accelerate your path to the next career level. The areas below will help you close any remaining gaps.");
        } else if (overall >= 2.5) {
            standing.put("body", "You have a solid professional foundation. This personalized plan is designed to help you accelerate growth in key areas and steadily improve your performance tier over the next 90 days.");
        } else {
            standing.put("body", "Some areas of your recent evaluation need focused attention. This plan provides clear, structured, and achievable steps to help you get back on track. Remember — consistent small improvements lead to big results.");
        }
        sections.add(standing);

        // Section 2: Technical Roadmap
        Map<String, String> techSection = new LinkedHashMap<>();
        techSection.put("title", "💻 Technical Growth Roadmap (Score: " + tech + "/5)");
        if (tech >= 5) {
            techSection.put("body", "• Deepen expertise by mentoring junior teammates — teaching accelerates your own mastery.\n• Pursue advanced certifications (Cloud Architecture, ML Engineering, System Design) to maintain a competitive edge.\n• Lead architectural decision-making and technical spikes in your department.");
        } else if (tech >= 4) {
            techSection.put("body", "• Take on 1 stretch technical challenge per quarter that pushes your current stack boundaries.\n• Explore cross-domain technologies to become a more versatile engineer.\n• Complete 2 advanced LMS certifications within the next 6 months.");
        } else if (tech >= 3) {
            techSection.put("body", "• Dedicate 1-2 hours per week to focused skill practice via the SmartHR LMS platform.\n• Pair-program with senior colleagues on live projects to learn real-world best practices.\n• Complete at least 1 core LMS course per month for the next 3 months.");
        } else {
            techSection.put("body", "• Immediately enroll in the Core Competency Bootcamp available in SmartHR LMS.\n• Set achievable daily learning goals — even 30 minutes of focused practice per day yields measurable results in 30 days.\n• Request a dedicated technical mentor from your direct manager.");
        }
        sections.add(techSection);

        // Section 3: Soft Skills Roadmap
        Map<String, String> softSection = new LinkedHashMap<>();
        softSection.put("title", "🤝 Interpersonal & Collaboration Goals (Score: " + soft + "/5)");
        if (soft >= 5) {
            softSection.put("body", "• Volunteer to lead cross-functional retrospectives and town halls to amplify your leadership presence.\n• Become a communication champion — help your team adopt better meeting hygiene and documentation practices.\n• Mentor colleagues on conflict resolution and stakeholder management techniques.");
        } else if (soft >= 4) {
            softSection.put("body", "• Actively pursue cross-department collaboration opportunities to broaden your professional network.\n• Offer structured, actionable feedback during team reviews to build your influence.\n• Volunteer to facilitate at least one team meeting per month.");
        } else if (soft >= 3) {
            softSection.put("body", "• Enroll in a structured communication or presentation skills workshop within the next 60 days.\n• Volunteer to run 1 team meeting per month to build confidence in a safe environment.\n• Practice active listening — summarize what others say before offering your response.");
        } else {
            softSection.put("body", "• Immediately join the SmartHR Soft Skills Development Track in the LMS.\n• Set up bi-weekly syncs with your manager specifically to practice and receive feedback on communication.\n• Focus on empathy-first listening in team interactions — ask clarifying questions before responding.");
        }
        sections.add(softSection);

        // Section 4: Attendance Goals
        Map<String, String> attnSection = new LinkedHashMap<>();
        attnSection.put("title", "⏰ Attendance & Reliability Targets (Score: " + attn + "/5)");
        if (attn >= 4) {
            attnSection.put("body", "• Excellent reliability! This professionalism is a key career differentiator.\n• Consider requesting flexible hours as a reward benefit for your consistency.\n• Maintain this track record across the next review cycle to reinforce your dependability profile.");
        } else {
            attnSection.put("body", "• Set a personal 30-day challenge: zero unplanned absences.\n• Use the SmartHR Leave Planner to schedule any foreseeable absences well in advance.\n• If recurring blockers (commute, health, personal) affect attendance, discuss them confidentially with HR for flexible solutions.");
        }
        sections.add(attnSection);

        // Section 5: 30-60-90 Day Plan
        Map<String, String> plan = new LinkedHashMap<>();
        plan.put("title", "🗓️ Your 30-60-90 Day Action Plan");
        plan.put("body", "• Day 1-30: Enroll in all recommended LMS courses. Schedule a career-alignment 1-on-1 with your manager. Define 3 SMART goals for this cycle.\n• Day 31-60: Complete your first major milestone. Request an informal mid-cycle performance check-in and collect feedback.\n• Day 61-90: Demonstrate measurable improvement in your development areas. Review this plan, celebrate progress, and set new ambitious targets for the next cycle.");
        sections.add(plan);

        Map<String, Object> result = new HashMap<>();
        result.put("tier", tier);
        result.put("tierIcon", tierIcon);
        result.put("overall", overall);
        result.put("sections", sections);
        result.put("reviewDate", latest.getReviewDate() != null ? latest.getReviewDate().toString() : "Recent");

        return ResponseEntity.ok(result);
    }

    // ──────────────────────────────────────────────
    // AI FEATURE 5: PerformanceBot AI Chat Assistant
    // POST /api/reviews/ai-assistant
    // ──────────────────────────────────────────────
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

        String aiResponse;
        String cleanMessage = userMessage.toLowerCase().trim();
        List<Review> allReviews = reviewRepository.findAll();

        if ("ADMIN".equals(user.getRole())) {
            // ── HR Admin: Performance Intelligence Bot ──
            if (cleanMessage.contains("top performer") || cleanMessage.contains("best employee") || cleanMessage.contains("star") || cleanMessage.contains("highest score")) {
                List<Review> topReviews = allReviews.stream()
                    .filter(r -> r.getOverallScore() != null && r.getOverallScore() >= 4.0)
                    .sorted((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()))
                    .limit(5).collect(Collectors.toList());

                if (topReviews.isEmpty()) {
                    aiResponse = "No reviews logged yet, or no employees have reached the 4.0+ performance threshold. Submit evaluations to identify your top performers.";
                } else {
                    StringBuilder sb = new StringBuilder("🏆 **Top Performing Employees** (Score ≥ 4.0):\n\n");
                    for (Review r : topReviews) {
                        String name = r.getEmployee() != null ? r.getEmployee().getFullName() : "Employee";
                        sb.append("- **").append(name).append("** — ").append(String.format("%.1f", r.getOverallScore())).append("/5.0\n");
                    }
                    sb.append("\n💡 *Consider these employees for promotions, recognition awards, or expanded leadership responsibilities.*");
                    aiResponse = sb.toString();
                }

            } else if (cleanMessage.contains("needs improvement") || cleanMessage.contains("low performer") || cleanMessage.contains("poor performance") || cleanMessage.contains("lowest score") || cleanMessage.contains("pip")) {
                List<Review> lowReviews = allReviews.stream()
                    .filter(r -> r.getOverallScore() != null && r.getOverallScore() < 3.0)
                    .sorted(Comparator.comparingDouble(r -> r.getOverallScore()))
                    .limit(5).collect(Collectors.toList());

                if (lowReviews.isEmpty()) {
                    aiResponse = "✅ Great news! No employees currently fall in the 'Needs Improvement' category (score < 3.0). Your team is performing well overall!";
                } else {
                    StringBuilder sb = new StringBuilder("⚠️ **Employees Requiring Performance Support** (Score < 3.0):\n\n");
                    for (Review r : lowReviews) {
                        String name = r.getEmployee() != null ? r.getEmployee().getFullName() : "Employee";
                        sb.append("- **").append(name).append("** — ").append(String.format("%.1f", r.getOverallScore())).append("/5.0\n");
                    }
                    sb.append("\n📋 *Recommendation: Initiate 30-day structured Performance Improvement Plans (PIP) and schedule bi-weekly 1-on-1 coaching sessions for each employee listed above.*");
                    aiResponse = sb.toString();
                }

            } else if (cleanMessage.contains("average") || cleanMessage.contains("overall score") || cleanMessage.contains("company performance") || cleanMessage.contains("summary") || cleanMessage.contains("overview")) {
                if (allReviews.isEmpty()) {
                    aiResponse = "No performance evaluation data is available yet. Begin logging evaluations through the form on this page to see company-wide AI analytics.";
                } else {
                    double avg = allReviews.stream().filter(r -> r.getOverallScore() != null).mapToDouble(Review::getOverallScore).average().orElse(0.0);
                    long elite = allReviews.stream().filter(r -> r.getOverallScore() != null && r.getOverallScore() >= 4.5).count();
                    long pip = allReviews.stream().filter(r -> r.getOverallScore() != null && r.getOverallScore() < 2.5).count();
                    int health = (int) Math.round(avg * 20.0);
                    String healthLabel = avg >= 4.0 ? "Excellent" : avg >= 3.0 ? "Good" : "Needs Attention";

                    aiResponse = "📊 **Company-Wide Performance Summary:**\n\n" +
                        "- **Average Score**: " + String.format("%.1f", avg) + " / 5.0\n" +
                        "- **Performance Health Index**: " + health + "% — " + healthLabel + "\n" +
                        "- **Elite Performers (≥4.5)**: " + elite + " employees\n" +
                        "- **Needs Support (<2.5)**: " + pip + " employees\n" +
                        "- **Total Evaluations Logged**: " + allReviews.size() + "\n\n" +
                        (avg >= 4.0 ? "💚 *Organizational performance is excellent. Sustain momentum through recognition and growth programs.*" :
                         avg >= 3.0 ? "💛 *Performance is good. Targeted training and coaching can push the team to elite levels.*" :
                         "🔴 *Immediate structured improvement programs and management interventions are advised.*");
                }

            } else if (cleanMessage.contains("promote") || cleanMessage.contains("promotion") || cleanMessage.contains("appraisal") || cleanMessage.contains("raise") || cleanMessage.contains("increment")) {
                List<Review> candidates = allReviews.stream()
                    .filter(r -> r.getOverallScore() != null && r.getOverallScore() >= 4.0)
                    .sorted((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()))
                    .limit(5).collect(Collectors.toList());

                if (candidates.isEmpty()) {
                    aiResponse = "No employees currently meet the ≥4.0 score threshold for promotion consideration. Encourage performance improvements and re-evaluate in the next cycle.";
                } else {
                    StringBuilder sb = new StringBuilder("🚀 **Promotion / Appraisal Candidates (Score ≥ 4.0):**\n\n");
                    for (Review r : candidates) {
                        String name = r.getEmployee() != null ? r.getEmployee().getFullName() : "Employee";
                        sb.append("- **").append(name).append("** — ").append(String.format("%.1f", r.getOverallScore())).append("/5.0 — ✅ Promotion Ready\n");
                    }
                    sb.append("\n💼 *Initiate a formal appraisal cycle for these employees and present the data to senior leadership for sign-off approval.*");
                    aiResponse = sb.toString();
                }

            } else if (cleanMessage.contains("training") || cleanMessage.contains("upskill") || cleanMessage.contains("course") || cleanMessage.contains("learning") || cleanMessage.contains("lms")) {
                aiResponse = "📚 **AI-Recommended Organizational Training Strategy:**\n\n" +
                    "Based on performance data patterns across your team:\n\n" +
                    "1. **Technical Skills Track**: Enroll employees with Technical scores ≤ 3 in domain-specific LMS bootcamps.\n" +
                    "2. **Soft Skills Workshop**: Employees with Soft Skill scores ≤ 3 benefit most from structured communication and leadership programs.\n" +
                    "3. **Attendance Coaching**: Address repeated absence patterns through confidential HR conversations and flexible scheduling.\n" +
                    "4. **High-Performer Acceleration**: Enroll top-tier employees in Advanced Leadership and Executive Development programs to retain and grow them.\n\n" +
                    "🎓 *Navigate to the **SmartHR LMS** module to assign specific courses directly to individual employees.*";

            } else if (cleanMessage.contains("hello") || cleanMessage.contains("hi") || cleanMessage.contains("help") || cleanMessage.contains("what can you")) {
                aiResponse = "👋 Hello! I am **PerformanceBot AI** — your intelligent HR Performance Analytics Advisor.\n\n" +
                    "Here's what I can help you with:\n\n" +
                    "- 🏆 *'Who are the top performers?'*\n" +
                    "- ⚠️ *'Who needs performance improvement?'*\n" +
                    "- 📊 *'Give me a company performance summary'*\n" +
                    "- 📚 *'Suggest a training strategy for the team'*\n" +
                    "- 🚀 *'Who is ready for promotion or appraisal?'*\n\n" +
                    "Ask me anything about your team's performance data!";
            } else {
                aiResponse = "I'm here to help you analyze and act on performance data. Try asking:\n\n" +
                    "- *'Show me the top performers'*\n" +
                    "- *'Who needs a performance improvement plan?'*\n" +
                    "- *'Give me a company-wide performance overview'*\n" +
                    "- *'What training programs should I run?'*\n" +
                    "- *'Who should be promoted this cycle?'*";
            }
        } else {
            // ── Employee: AI Career Coach ──
            if (cleanMessage.contains("improve") || cleanMessage.contains("better") || cleanMessage.contains("development") || cleanMessage.contains("grow") || cleanMessage.contains("progress")) {
                aiResponse = "🌱 **Your Career Growth Accelerator Tips:**\n\n" +
                    "1. **Skill Building**: Use SmartHR LMS to complete at least 1 course per month in your domain.\n" +
                    "2. **Feedback Loop**: After every project, ask your manager for structured, specific feedback — it identifies blind spots faster.\n" +
                    "3. **Visibility**: Volunteer for cross-functional projects to build your organizational network and show range.\n" +
                    "4. **SMART Goals**: Set Specific, Measurable, Achievable, Relevant, Time-bound goals at the start of each month.\n\n" +
                    "💡 *Scroll down to generate your full AI Development Plan personalized to your evaluation scores!*";

            } else if (cleanMessage.contains("score") || cleanMessage.contains("rating") || cleanMessage.contains("evaluation") || cleanMessage.contains("review") || cleanMessage.contains("mean")) {
                aiResponse = "📊 **Understanding Your Performance Evaluation:**\n\n" +
                    "Your scores are measured across three dimensions:\n" +
                    "- **Technical (1-5)**: Domain knowledge, output quality, and problem-solving.\n" +
                    "- **Soft Skills (1-5)**: Communication, teamwork, and emotional intelligence.\n" +
                    "- **Attendance (1-5)**: Reliability, punctuality, and professional consistency.\n\n" +
                    "**Performance Tiers:**\n" +
                    "- 🏆 4.5+ → Elite Performer (Top 10%)\n" +
                    "- ⭐ 3.5–4.4 → High Performer\n" +
                    "- 📈 2.5–3.4 → Developing Performer\n" +
                    "- 🎯 < 2.5 → Needs Structured Support\n\n" +
                    "💡 *Scroll down and click \"Generate My AI Development Plan\" for a personalized roadmap!*";

            } else if (cleanMessage.contains("promotion") || cleanMessage.contains("promote") || cleanMessage.contains("raise") || cleanMessage.contains("salary") || cleanMessage.contains("advance")) {
                aiResponse = "🚀 **How to Position Yourself for Promotion:**\n\n" +
                    "1. **Hit 4.0+ consistently** across at least 2 consecutive review cycles.\n" +
                    "2. **Own high-impact deliverables** — volunteer for projects visible to senior leadership.\n" +
                    "3. **Excel in soft skills** — be the person who resolves conflicts and unblocks the team.\n" +
                    "4. **Upskill proactively** — complete advanced LMS certifications to strengthen your professional profile.\n" +
                    "5. **Have the conversation** — schedule an appraisal discussion with your manager backed by your performance data.\n\n" +
                    "💪 *Employees who proactively discuss their growth goals with managers are 3× more likely to receive timely promotions.*";

            } else if (cleanMessage.contains("stress") || cleanMessage.contains("overwhelm") || cleanMessage.contains("burnout") || cleanMessage.contains("tired") || cleanMessage.contains("exhausted")) {
                aiResponse = "🌿 **It's okay — here's how to manage workplace stress:**\n\n" +
                    "1. **Talk to HR**: Use our confidential HR Buddy module to discuss workload concerns privately.\n" +
                    "2. **Prioritize Tasks**: Use a simple priority matrix (Urgent/Important quadrant) to focus only on what matters most today.\n" +
                    "3. **Use Your Leave**: You have leave entitlements — use them! Regular restorative breaks prevent long-term burnout.\n" +
                    "4. **Set Capacity Limits**: Communicate clearly: *'I can deliver Task A by Friday, but Task B would need to shift to next week to maintain quality.'*\n\n" +
                    "🤝 *Your performance scores don't define your worth as a person. Sustainable, consistent performance is far more valuable than short-term overwork.*";

            } else if (cleanMessage.contains("hello") || cleanMessage.contains("hi") || cleanMessage.contains("help") || cleanMessage.contains("what can you")) {
                aiResponse = "👋 Hello! I am your **AI Career Coach** — here to support your professional growth journey.\n\n" +
                    "I can help you with:\n" +
                    "- 📈 *'How can I improve my performance?'*\n" +
                    "- 📊 *'What do my evaluation scores mean?'*\n" +
                    "- 🚀 *'How do I position myself for a promotion?'*\n" +
                    "- 🌿 *'I'm feeling stressed and overwhelmed — help!'*\n\n" +
                    "Also, don't forget to **Generate your AI Development Plan** below — it's built specifically for you based on your latest review!";
            } else {
                aiResponse = "Thank you for reaching out! 🌟 As your AI Career Coach, I'm here to support your professional journey every step of the way.\n\n" +
                    "Try asking me about:\n" +
                    "- How to improve your performance evaluation scores\n" +
                    "- What your current ratings mean and how to interpret them\n" +
                    "- How to position yourself for promotion or a salary increment\n" +
                    "- Managing work-related stress and burnout\n\n" +
                    "Or scroll down to **Generate your personalized AI Development Plan!** 🎯";
            }
        }

        return ResponseEntity.ok(Collections.singletonMap("response", aiResponse));
    }
}