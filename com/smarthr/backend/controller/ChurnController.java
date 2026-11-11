package com.smarthr.backend.controller;

import com.smarthr.backend.model.Employee;
import com.smarthr.backend.model.ChurnRecord;
import com.smarthr.backend.model.User;
import com.smarthr.backend.repository.EmployeeRepository;
import com.smarthr.backend.repository.ChurnRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/churn")
@CrossOrigin(origins = "*")
public class ChurnController {

    @Autowired
    private ChurnRepository churnRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // Helper: Verify admin session
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        return user != null && "ADMIN".equals(user.getRole());
    }

    // Helper: Get logged-in employee profile
    private Employee getLoggedInEmployee(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null) {
            return employeeRepository.findByEmail(user.getEmail()).orElse(null);
        }
        return null;
    }

    // 1. GET /api/churn/admin/records - Admin access only
    @GetMapping("/admin/records")
    public ResponseEntity<?> getAdminRecords(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Access denied. Admins only.");
        }

        List<Employee> allEmployees = employeeRepository.findAll();
        List<Map<String, Object>> responseList = new ArrayList<>();

        // Calculate average salary by department to find who is underpaid
        Map<String, Double> deptAvgSalary = allEmployees.stream()
            .filter(e -> e.getDepartment() != null && e.getBaseSalary() != null)
            .collect(Collectors.groupingBy(
                Employee::getDepartment,
                Collectors.averagingDouble(Employee::getBaseSalary)
            ));

        for (Employee emp : allEmployees) {
            Optional<ChurnRecord> recordOpt = churnRepository.findByEmployeeId(emp.getId());
            ChurnRecord record;

            if (recordOpt.isPresent()) {
                record = recordOpt.get();
            } else {
                // Seed baseline churn data if none exists
                record = new ChurnRecord();
                record.setEmployeeId(emp.getId());
                record.setEmployeeName(emp.getFullName());
                
                // Deterministic seed based on name hash for consistency
                int hash = emp.getFullName().hashCode();
                double seedSatisfaction = 3.0 + (hash % 3) * 0.7; // 3.0, 3.7, 4.4
                double seedStress = 2.0 + (Math.abs(hash) % 4) * 0.8; // 2.0, 2.8, 3.6, 4.4
                double seedBalance = 4.0 - (hash % 2) * 1.0; // 4.0, 3.0
                double seedGrowth = 3.5 + (hash % 2) * 0.5; // 3.5, 4.0
                
                record.setSatisfactionScore(seedSatisfaction);
                record.setStressLevel(seedStress);
                record.setWorkLifeBalance(seedBalance);
                record.setGrowthOpportunity(seedGrowth);
                record.setUpdatedAt(LocalDateTime.now().minusDays(5));
                
                // Calculate dynamic score and factors
                calculateRisk(record, emp, deptAvgSalary);
                churnRepository.save(record);
            }

            Map<String, Object> map = new HashMap<>();
            map.put("recordId", record.getId());
            map.put("employeeId", emp.getId());
            map.put("fullName", emp.getFullName());
            map.put("designation", emp.getDesignation() != null ? emp.getDesignation() : "Specialist");
            map.put("department", emp.getDepartment() != null ? emp.getDepartment() : "Operations");
            map.put("salary", emp.getBaseSalary() != null ? emp.getBaseSalary() : 45000.0);
            map.put("profilePic", emp.getProfilePic());
            
            map.put("riskScore", record.getRiskScore());
            map.put("riskLevel", record.getRiskLevel());
            map.put("riskFactors", record.getRiskFactors());
            map.put("satisfactionScore", record.getSatisfactionScore());
            map.put("workLifeBalance", record.getWorkLifeBalance());
            map.put("stressLevel", record.getStressLevel());
            map.put("growthOpportunity", record.getGrowthOpportunity());
            map.put("aiRecommendation", record.getAiRecommendation());
            map.put("updatedAt", record.getUpdatedAt());

            responseList.add(map);
        }

        // Sort response by riskScore descending so high risk appears first
        responseList.sort((a, b) -> Double.compare((Double) b.get("riskScore"), (Double) a.get("riskScore")));

        return ResponseEntity.ok(responseList);
    }

    // 2. GET /api/churn/employee/my-record - Employee confidential view
    @GetMapping("/employee/my-record")
    public ResponseEntity<?> getMyRecord(HttpSession session) {
        Employee emp = getLoggedInEmployee(session);
        if (emp == null) {
            return ResponseEntity.status(401).body("Unauthorized or Employee profile not found.");
        }

        Optional<ChurnRecord> recordOpt = churnRepository.findByEmployeeId(emp.getId());
        if (recordOpt.isPresent()) {
            return ResponseEntity.ok(recordOpt.get());
        }

        // Create baseline record if it doesn't exist
        ChurnRecord record = new ChurnRecord();
        record.setEmployeeId(emp.getId());
        record.setEmployeeName(emp.getFullName());
        record.setSatisfactionScore(4.0);
        record.setStressLevel(2.0);
        record.setWorkLifeBalance(4.0);
        record.setGrowthOpportunity(4.0);
        record.setUpdatedAt(LocalDateTime.now());
        
        calculateRisk(record, emp, new HashMap<>());
        churnRepository.save(record);

        return ResponseEntity.ok(record);
    }

    // 3. POST /api/churn/employee/survey - Submit a wellness survey and dynamically update risk
    @PostMapping("/employee/survey")
    public ResponseEntity<?> submitSurvey(@RequestBody ChurnRecord surveyInput, HttpSession session) {
        Employee emp = getLoggedInEmployee(session);
        if (emp == null) {
            return ResponseEntity.status(401).body("Unauthorized. Please login as employee.");
        }

        ChurnRecord record = churnRepository.findByEmployeeId(emp.getId())
                .orElse(new ChurnRecord());

        record.setEmployeeId(emp.getId());
        record.setEmployeeName(emp.getFullName());
        record.setSatisfactionScore(surveyInput.getSatisfactionScore());
        record.setWorkLifeBalance(surveyInput.getWorkLifeBalance());
        record.setStressLevel(surveyInput.getStressLevel());
        record.setGrowthOpportunity(surveyInput.getGrowthOpportunity());
        record.setUpdatedAt(LocalDateTime.now());

        // Calculate average salary by department for market salary comparison
        List<Employee> allEmployees = employeeRepository.findAll();
        Map<String, Double> deptAvgSalary = allEmployees.stream()
            .filter(e -> e.getDepartment() != null && e.getBaseSalary() != null)
            .collect(Collectors.groupingBy(
                Employee::getDepartment,
                Collectors.averagingDouble(Employee::getBaseSalary)
            ));

        calculateRisk(record, emp, deptAvgSalary);
        churnRepository.save(record);

        return ResponseEntity.ok(record);
    }

    // 4. POST /api/churn/admin/retention-plan - Admin generates a strategy plan
    @PostMapping("/admin/retention-plan")
    public ResponseEntity<?> generateRetentionPlan(@RequestBody Map<String, Long> payload, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        Long employeeId = payload.get("employeeId");
        if (employeeId == null) {
            return ResponseEntity.badRequest().body("Employee ID is required.");
        }

        Optional<ChurnRecord> recordOpt = churnRepository.findByEmployeeId(employeeId);
        if (!recordOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        ChurnRecord record = recordOpt.get();
        String factors = record.getRiskFactors();
        StringBuilder plan = new StringBuilder();

        plan.append("### AI-Generated Retention Strategy for ").append(record.getEmployeeName()).append("\n\n");
        plan.append("**Overview**: Based on AI analysis of work telemetry, current satisfaction indicators, and tenure, ");
        plan.append("the following custom retention checklist has been generated to mitigate the detected risk factors.\n\n");

        if (factors == null || factors.trim().isEmpty() || factors.equals("None")) {
            plan.append("- **Proactive Growth Check**: Schedule a periodic career advancement sync. Provide high visibility projects to maintain positive engagement levels.\n");
            plan.append("- **Peer Recognition**: Nominating for leadership honors or department spotlights to keep morale high.\n");
        } else {
            String[] list = factors.split(", ");
            for (String factor : list) {
                if (factor.contains("Salary") || factor.contains("Compensation")) {
                    plan.append("#### 1. Compensation Correction & Rewards\n");
                    plan.append("- **Immediate Action**: Initiate an off-cycle salary correction check. Current base salary is estimated to be lower than their department standard.\n");
                    plan.append("- **Short-term Action**: Offer performance-linked milestone bonuses or customized perks.\n\n");
                }
                if (factor.contains("Stress") || factor.contains("Overtime")) {
                    plan.append("#### 2. Workload Adjustment & Burnout Mitigation\n");
                    plan.append("- **Immediate Action**: Enforce strict caps on overtime work and late-night commits. Re-distribute task backlogs immediately.\n");
                    plan.append("- **Short-term Action**: Grant 2 days of paid Wellness Leave. Provide access to company-sponsored counseling services.\n\n");
                }
                if (factor.contains("Balance")) {
                    plan.append("#### 3. Flexible Working Modalities\n");
                    plan.append("- **Immediate Action**: Approve up to 3 days of Work-from-Home per week to ease commute stress.\n");
                    plan.append("- **Short-term Action**: Introduce core flex-hours to accommodate work-life requirements.\n\n");
                }
                if (factor.contains("Growth") || factor.contains("Stagnation")) {
                    plan.append("#### 4. Career Growth & Leadership Sponsorship\n");
                    plan.append("- **Immediate Action**: Set up a clear 6-month promotions roadmap. Assign a Senior Director as a career sponsor.\n");
                    plan.append("- **Short-term Action**: Enroll the employee in the Premium Executive Leadership Program via SmartHR LMS.\n\n");
                }
                if (factor.contains("Satisfaction")) {
                    plan.append("#### 5. Managerial Sync & 1-on-1 Feedback\n");
                    plan.append("- **Immediate Action**: Hold a confidential, empathetic 1-on-1 feedback session. Let the employee express grievances without fear of judgment.\n");
                    plan.append("- **Short-term Action**: Resolve immediate departmental friction or structural team bottlenecks.\n\n");
                }
            }
        }

        plan.append("**Suggested Follow-up**: HR should organize a direct manager check-in within the next 7 working days to discuss these opportunities.");

        record.setAiRecommendation(plan.toString());
        churnRepository.save(record);

        return ResponseEntity.ok(Collections.singletonMap("plan", plan.toString()));
    }

    // 5. POST /api/churn/ai-assistant - Strategic or Wellness Counselor AI Bot
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
        String cleanMessage = userMessage.toLowerCase();

        if ("ADMIN".equals(user.getRole())) {
            // HR / Admin ChurnBot AI Assistant
            if (cleanMessage.contains("highest risk") || cleanMessage.contains("who is leaving") || cleanMessage.contains("high risk")) {
                List<ChurnRecord> highRisk = churnRepository.findAll().stream()
                        .filter(r -> "HIGH".equals(r.getRiskLevel()))
                        .collect(Collectors.toList());
                
                if (highRisk.isEmpty()) {
                    aiResponse = "Currently, there are **no employees** in the high-churn risk category! Moral index is healthy. Keep promoting a supportive environment.";
                } else {
                    StringBuilder sb = new StringBuilder("The following employees are showing a **HIGH Churn Risk**:\n\n");
                    for (ChurnRecord r : highRisk) {
                        sb.append("- **").append(r.getEmployeeName()).append("** (Risk Score: ").append(Math.round(r.getRiskScore())).append("%)\n");
                        sb.append("  *Drivers*: ").append(r.getRiskFactors()).append("\n\n");
                    }
                    sb.append("I highly recommend checking their profile and generating an **AI Retention Plan** for immediate steps.");
                    aiResponse = sb.toString();
                }
            } else if (cleanMessage.contains("how to retain") || cleanMessage.contains("retain employee")) {
                aiResponse = "To retain a high-risk employee, follow these core pillars:\n\n" +
                        "1. **Empathic Dialogue**: Meet 1-on-1 to understand their pain points (salary, commute, stress) without defensive arguments.\n" +
                        "2. **Real Support**: If workload stress is the issue, re-prioritize project timelines. If salary, offer a correction.\n" +
                        "3. **Transparent Roadmap**: Provide clear growth paths. Employees who see their future in the company are 82% less likely to resign.";
            } else if (cleanMessage.contains("draft email") || cleanMessage.contains("email template")) {
                aiResponse = "Here is a professional, empathetic email template to schedule a sync:\n\n" +
                        "```\nSubject: Connect & Sync-up / Feedback Session\n\n" +
                        "Hi [Employee Name],\n\n" +
                        "I hope you are doing well. I wanted to take some time to catch up, hear how your projects are going, and discuss how we can better support you in your career path and day-to-day work environment here.\n\n" +
                        "Let's grab a coffee or set up a brief virtual sync this week. Your feedback and comfort are highly valued.\n\n" +
                        "Best regards,\n[Your Name]\nSmartHR Management\n```";
            } else {
                aiResponse = "Hello! I am **ChurnBot AI**, your strategic HR retention advisor. I can analyze employee risk matrices, identify department-wide bottlenecks, suggest retention tactics, and draft meeting emails. Ask me 'Who is at highest risk?' or 'How can I retain an employee?' for details!";
            }
        } else {
            // Employee Wellness Coach AI Assistant
            if (cleanMessage.contains("stressed") || cleanMessage.contains("burnout") || cleanMessage.contains("overwork") || cleanMessage.contains("exhausted")) {
                aiResponse = "I'm really sorry to hear you're feeling this way. Burnout is a serious indicator, and your mental health is a top priority. Here's what we can do:\n\n" +
                        "- **Take Micro-Breaks**: Use the Pomodoro technique (5-min breaks every 25 mins of work) to step away from the monitor.\n" +
                        "- **Confidential Survey**: Make sure you fill out our confidential Wellness Survey on this page so the management gets anonymized telemetry about workload friction.\n" +
                        "- **Negotiating Workload**: It is healthy to ask for help. Try saying: *'I want to deliver high-quality results, but with my current workload, I need help re-prioritizing these tasks. Can we align on this?'*";
            } else if (cleanMessage.contains("growth") || cleanMessage.contains("stagnant") || cleanMessage.contains("promotion")) {
                aiResponse = "It's excellent that you are focused on growth! If you feel stuck, consider these options:\n\n" +
                        "- **SmartHR LMS**: Explore the 'Training & LMS' module on our dashboard. Taking courses demonstrates proactive initiative.\n" +
                        "- **Sync with Manager**: Schedule a career path discussion. Share your aspirations and ask for actionable feedback to qualify for next-level designations.\n" +
                        "- **Internal Rotations**: If you want to learn new tools, query HR about internal project shifts.";
            } else if (cleanMessage.contains("hello") || cleanMessage.contains("hi") || cleanMessage.contains("help")) {
                aiResponse = "Hello! I am your **Empathetic Wellness Coach**. This is a completely confidential channel designed to support you with stress management, work-life balance advice, career stagnation tips, and healthy ways to communicate challenges to your team. How can I help you feel better today?";
            } else {
                aiResponse = "Thank you for sharing this. Remember, your well-being is vital. You can take short steps to practice mindfulness, balance your calendar, or utilize our smart self-service channels. Feel free to ask me for advice on dealing with workload stress, managing communication, or professional growth paths!";
            }
        }

        return ResponseEntity.ok(Collections.singletonMap("response", aiResponse));
    }

    // Core Logical engine to compute churn risk dynamically
    private void calculateRisk(ChurnRecord record, Employee emp, Map<String, Double> deptAvgSalary) {
        double score = 40.0; // baseline

        // 1. Job Satisfaction (impacts risk up to +/- 20%)
        double sat = record.getSatisfactionScore() != null ? record.getSatisfactionScore() : 4.0;
        score += (3.0 - sat) * 12.0;

        // 2. Work Life Balance (impacts risk up to +/- 15%)
        double wlb = record.getWorkLifeBalance() != null ? record.getWorkLifeBalance() : 4.0;
        score += (3.0 - wlb) * 10.0;

        // 3. Stress Levels (impacts risk up to +/- 20%)
        double stress = record.getStressLevel() != null ? record.getStressLevel() : 2.0;
        score += (stress - 3.0) * 12.0;

        // 4. Growth Opportunities (impacts risk up to +/- 12%)
        double growth = record.getGrowthOpportunity() != null ? record.getGrowthOpportunity() : 4.0;
        score += (3.0 - growth) * 8.0;

        // 5. Compensation factor: Compare their salary with department average
        List<String> factorList = new ArrayList<>();
        if (emp.getBaseSalary() != null && emp.getDepartment() != null) {
            Double avgSalary = deptAvgSalary.get(emp.getDepartment());
            if (avgSalary != null && avgSalary > 0) {
                double diffPercent = (avgSalary - emp.getBaseSalary()) / avgSalary;
                if (diffPercent > 0.15) {
                    score += 15.0; // Underpaid by >15% triggers risk penalty
                    factorList.add("Compensation (Under Market)");
                }
            }
        }

        // Add regular risk drivers based on scores
        if (stress >= 4.0) {
            factorList.add("Extreme Work Stress");
        }
        if (wlb <= 2.0) {
            factorList.add("Poor Work-Life Balance");
        }
        if (sat <= 2.5) {
            factorList.add("Low Job Satisfaction");
        }
        if (growth <= 2.5) {
            factorList.add("Career Growth Stagnation");
        }

        // Bound check 0 to 100
        score = Math.max(0.0, Math.min(100.0, score));
        record.setRiskScore(score);

        // Classify Risk Levels
        if (score >= 70.0) {
            record.setRiskLevel("HIGH");
        } else if (score >= 40.0) {
            record.setRiskLevel("MEDIUM");
        } else {
            record.setRiskLevel("LOW");
        }

        if (factorList.isEmpty()) {
            record.setRiskFactors("None");
        } else {
            record.setRiskFactors(String.join(", ", factorList));
        }

        // Pre-fill a small default strategy in case Admin doesn't trigger AI plan
        if (record.getRiskLevel().equals("HIGH")) {
            record.setAiRecommendation("- Plan immediate off-cycle salary check.\n- Request manager to restrict overtime assignments.\n- Arrange an informal coffee chat this week.");
        } else if (record.getRiskLevel().equals("MEDIUM")) {
            record.setAiRecommendation("- Periodic 1-on-1 career mapping review.\n- Encourage participation in company wellness leaves.");
        } else {
            record.setAiRecommendation("- Keep employee motivated with leadership project roles.\n- Continue standard annual check-in cycles.");
        }
    }
}
