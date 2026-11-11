package com.smarthr.backend.controller;

import com.smarthr.backend.model.Employee;
import com.smarthr.backend.model.SmartAlert;
import com.smarthr.backend.model.User;
import com.smarthr.backend.repository.EmployeeRepository;
import com.smarthr.backend.repository.SmartAlertRepository;
import com.smarthr.backend.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class SmartAlertController {

    @Autowired
    private SmartAlertRepository alertRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

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

    // Auto-seed baseline alerts for all database employees if none exist
    private void seedAlertsIfEmpty() {
        if (alertRepository.count() == 0) {
            List<Employee> employees = employeeRepository.findAll();
            List<SmartAlert> seedList = new ArrayList<>();

            for (Employee emp : employees) {
                // Find corresponding user to check 2FA status
                Optional<User> userOpt = userRepository.findAll().stream()
                        .filter(u -> u.getEmail().equalsIgnoreCase(emp.getEmail()))
                        .findFirst();
                boolean twoFactor = userOpt.isPresent() && userOpt.get().isTwoFactorEnabled();

                // Alert 1: 2FA Compliance (Predictive security audit)
                if (!twoFactor) {
                    SmartAlert securityAlert = new SmartAlert();
                    securityAlert.setEmployeeId(emp.getId());
                    securityAlert.setEmployeeName(emp.getFullName());
                    securityAlert.setTitle("Security Compliance: 2FA Disabled");
                    securityAlert.setMessage("AI Security Audit has detected that 2-Factor Authentication is disabled on your account. This violates section 4.2 of the corporate Security Policy.");
                    securityAlert.setType("COMPLIANCE");
                    securityAlert.setSeverity("HIGH");
                    securityAlert.setResolved(false);
                    securityAlert.setCreatedAt(LocalDateTime.now().minusDays(2));
                    securityAlert.setDeadline(LocalDateTime.now().plusDays(3));
                    securityAlert.setAiReasoning("Based on credential scans, 2-Factor Authentication is currently inactive for this profile. Dynamic security profiling predicts an 84% probability of unauthorized access attempts in case of email credential leaks. Recommended actions: Require user to setup 2FA immediately to satisfy enterprise audit rules.");
                    seedList.add(securityAlert);
                }

                // Alert 2: LMS Training Deadline
                SmartAlert lmsAlert = new SmartAlert();
                lmsAlert.setEmployeeId(emp.getId());
                lmsAlert.setEmployeeName(emp.getFullName());
                lmsAlert.setTitle("LMS Course Overdue: Code of Conduct Training");
                lmsAlert.setMessage("Your assigned compliance module 'Professional Code of Conduct' has passed its deadline.");
                lmsAlert.setType("DEADLINE");
                lmsAlert.setSeverity("MEDIUM");
                lmsAlert.setResolved(false);
                lmsAlert.setCreatedAt(LocalDateTime.now().minusDays(5));
                lmsAlert.setDeadline(LocalDateTime.now().minusDays(1));
                lmsAlert.setAiReasoning("Training progress is stuck at 0% after 3 automated reminders. Employee's average daily tasks are 15% higher than their peer average, indicating workflow overload is the bottleneck. Recommendation: manager should grant a 2-hour learning buffer window during core working hours.");
                seedList.add(lmsAlert);

                // Alert 3: Tax Declaration (Payroll)
                SmartAlert payrollAlert = new SmartAlert();
                payrollAlert.setEmployeeId(emp.getId());
                payrollAlert.setEmployeeName(emp.getFullName());
                payrollAlert.setTitle("Payroll Deadline: Tax Investment Declarations");
                payrollAlert.setMessage("Annual investment proofs and declarations for FY 2026 must be uploaded to avoid default deduction tiers.");
                payrollAlert.setType("PAYROLL");
                payrollAlert.setSeverity("HIGH");
                payrollAlert.setResolved(false);
                payrollAlert.setCreatedAt(LocalDateTime.now().minusDays(1));
                payrollAlert.setDeadline(LocalDateTime.now().plusDays(5));
                payrollAlert.setAiReasoning("Missing uploads detected in payroll workspace. AI forecasting predicts a default withholding of 22% on the upcoming pay slip if submissions aren't completed. Recommendation: Send dynamic push notification alerting user of impending financial impact.");
                seedList.add(payrollAlert);

                // Alert 4: Performance Review Sync
                SmartAlert reviewAlert = new SmartAlert();
                reviewAlert.setEmployeeId(emp.getId());
                reviewAlert.setEmployeeName(emp.getFullName());
                reviewAlert.setTitle("Schedule Q2 Performance Review");
                reviewAlert.setMessage("Schedule your Q2 career alignment and performance feedback sync with your reporting manager.");
                reviewAlert.setType("SYSTEM");
                reviewAlert.setSeverity("LOW");
                reviewAlert.setResolved(false);
                reviewAlert.setCreatedAt(LocalDateTime.now().minusDays(3));
                reviewAlert.setDeadline(LocalDateTime.now().plusDays(10));
                reviewAlert.setAiReasoning("Bi-annual review window is open. Telemetry shows standard review completion rate in this department is below average. Early scheduling is advised to prevent timeline overrun.");
                seedList.add(reviewAlert);
            }

            // Seed generic Admin Alerts if there are no employee alerts
            if (seedList.isEmpty()) {
                SmartAlert adminAlert1 = new SmartAlert();
                adminAlert1.setEmployeeId(0L);
                adminAlert1.setEmployeeName("HR System");
                adminAlert1.setTitle("Enterprise Compliance Audit Approaching");
                adminAlert1.setMessage("Quarterly system-wide compliance levels are at 78%. Action required to reach target 95%.");
                adminAlert1.setType("COMPLIANCE");
                adminAlert1.setSeverity("HIGH");
                adminAlert1.setResolved(false);
                adminAlert1.setCreatedAt(LocalDateTime.now());
                adminAlert1.setDeadline(LocalDateTime.now().plusDays(15));
                adminAlert1.setAiReasoning("Compliance metrics are down due to low course completion rates. Setting automated enforcement will boost scores by 20% in 10 days.");
                seedList.add(adminAlert1);
            }

            alertRepository.saveAll(seedList);
        }
    }

    // 1. GET /api/alerts/admin/all - View all alerts for all employees (Admin only)
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllAlerts(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Access denied. Admins only.");
        }
        seedAlertsIfEmpty();
        List<SmartAlert> alerts = alertRepository.findAll();
        // Sort: unresolved first, then High severity first, then newest first
        alerts.sort((a, b) -> {
            if (a.isResolved() != b.isResolved()) {
                return Boolean.compare(a.isResolved(), b.isResolved());
            }
            int severityComparison = getSeverityWeight(b.getSeverity()) - getSeverityWeight(a.getSeverity());
            if (severityComparison != 0) {
                return severityComparison;
            }
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        return ResponseEntity.ok(alerts);
    }

    private int getSeverityWeight(String severity) {
        if (severity == null) return 0;
        switch (severity.toUpperCase()) {
            case "HIGH": return 3;
            case "MEDIUM": return 2;
            case "LOW": return 1;
            default: return 0;
        }
    }

    // 2. GET /api/alerts/employee/my-alerts - View alerts for current logged-in employee
    @GetMapping("/employee/my-alerts")
    public ResponseEntity<?> getMyAlerts(HttpSession session) {
        Employee emp = getLoggedInEmployee(session);
        if (emp == null) {
            return ResponseEntity.status(401).body("Unauthorized. Employee profile not found.");
        }
        seedAlertsIfEmpty();
        List<SmartAlert> alerts = alertRepository.findByEmployeeId(emp.getId());
        alerts.sort((a, b) -> {
            if (a.isResolved() != b.isResolved()) {
                return Boolean.compare(a.isResolved(), b.isResolved());
            }
            return getSeverityWeight(b.getSeverity()) - getSeverityWeight(a.getSeverity());
        });
        return ResponseEntity.ok(alerts);
    }

    // 3. PUT /api/alerts/{id}/resolve - Resolve an alert
    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveAlert(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized session.");
        }

        Optional<SmartAlert> alertOpt = alertRepository.findById(id);
        if (!alertOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        SmartAlert alert = alertOpt.get();
        
        // Employees can only resolve their own alerts. Admins can resolve any.
        if (!"ADMIN".equals(user.getRole())) {
            Employee emp = getLoggedInEmployee(session);
            if (emp == null || !emp.getId().equals(alert.getEmployeeId())) {
                return ResponseEntity.status(403).body("Access Denied. Cannot resolve this alert.");
            }
        }

        alert.setResolved(true);
        alertRepository.save(alert);
        return ResponseEntity.ok(alert);
    }

    // 4. DELETE /api/alerts/{id} - Delete alert (Admin only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAlert(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Access denied.");
        }
        if (alertRepository.existsById(id)) {
            alertRepository.deleteById(id);
            return ResponseEntity.ok(Collections.singletonMap("success", true));
        }
        return ResponseEntity.notFound().build();
    }

    // 5. POST /api/alerts/admin/create - Admin manually triggers/creates an alert
    @PostMapping("/admin/create")
    public ResponseEntity<?> createAlert(@RequestBody SmartAlert newAlert, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        Optional<Employee> empOpt = employeeRepository.findById(newAlert.getEmployeeId());
        if (!empOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Invalid Employee ID.");
        }

        Employee emp = empOpt.get();
        newAlert.setEmployeeName(emp.getFullName());
        newAlert.setResolved(false);
        newAlert.setCreatedAt(LocalDateTime.now());
        if (newAlert.getDeadline() == null) {
            newAlert.setDeadline(LocalDateTime.now().plusDays(7));
        }

        // Generate AI Reasoning if not provided
        if (newAlert.getAiReasoning() == null || newAlert.getAiReasoning().trim().isEmpty()) {
            String reasoning = "Predictive compliance assessment initialized manually for " + emp.getFullName() + ". " +
                    "AI agent evaluates priority weight as " + newAlert.getSeverity() + " based on category " + newAlert.getType() + ". " +
                    "Projected SLA resolution benchmark is set to " + newAlert.getDeadline() + " to minimize systemic risk metrics.";
            newAlert.setAiReasoning(reasoning);
        }

        alertRepository.save(newAlert);
        return ResponseEntity.ok(newAlert);
    }

    // 6. GET /api/alerts/stats - Retrieve alert stats for KPIs
    @GetMapping("/stats")
    public ResponseEntity<?> getAlertStats(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized.");
        }

        seedAlertsIfEmpty();
        List<SmartAlert> allAlerts;
        if ("ADMIN".equals(user.getRole())) {
            allAlerts = alertRepository.findAll();
        } else {
            Employee emp = getLoggedInEmployee(session);
            allAlerts = emp != null ? alertRepository.findByEmployeeId(emp.getId()) : new ArrayList<>();
        }

        long total = allAlerts.size();
        long pending = allAlerts.stream().filter(a -> !a.isResolved()).count();
        long resolved = total - pending;
        long highSeverity = allAlerts.stream().filter(a -> "HIGH".equals(a.getSeverity()) && !a.isResolved()).count();
        long mediumSeverity = allAlerts.stream().filter(a -> "MEDIUM".equals(a.getSeverity()) && !a.isResolved()).count();
        long lowSeverity = allAlerts.stream().filter(a -> "LOW".equals(a.getSeverity()) && !a.isResolved()).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("resolved", resolved);
        stats.put("highSeverity", highSeverity);
        stats.put("mediumSeverity", mediumSeverity);
        stats.put("lowSeverity", lowSeverity);

        // Compliance score calculated as percentage of resolved alerts
        double complianceScore = total == 0 ? 100.0 : ((double) resolved / total) * 100.0;
        stats.put("complianceScore", complianceScore);

        return ResponseEntity.ok(stats);
    }

    // 7. POST /api/alerts/ai-advisor - Strategic compliance counselor bot
    @PostMapping("/ai-advisor")
    public ResponseEntity<?> aiAdvisor(@RequestBody Map<String, String> payload, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthenticated session.");
        }

        String userMessage = payload.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message is empty.");
        }

        String aiResponse;
        String cleanMessage = userMessage.toLowerCase();
        
        List<SmartAlert> activeAlerts;
        if ("ADMIN".equals(user.getRole())) {
            activeAlerts = alertRepository.findByResolved(false);
        } else {
            Employee emp = getLoggedInEmployee(session);
            activeAlerts = emp != null ? alertRepository.findByEmployeeIdAndResolved(emp.getId(), false) : new ArrayList<>();
        }

        if (cleanMessage.contains("highest severity") || cleanMessage.contains("high severity") || cleanMessage.contains("critical") || cleanMessage.contains("risks")) {
            List<SmartAlert> highSeverity = activeAlerts.stream()
                    .filter(a -> "HIGH".equals(a.getSeverity()))
                    .collect(Collectors.toList());

            if (highSeverity.isEmpty()) {
                aiResponse = "Currently, there are **no high-severity pending alerts**! Enterprise compliance metrics are fully secure and in the optimal range.";
            } else {
                StringBuilder sb = new StringBuilder("The following critical alerts require **IMMEDIATE resolution**:\n\n");
                for (SmartAlert a : highSeverity) {
                    sb.append("- **").append(a.getTitle()).append("** assigned to **").append(a.getEmployeeName()).append("**\n");
                    sb.append("  *Context*: ").append(a.getMessage()).append("\n");
                    sb.append("  *AI Recommendation*: ").append(a.getAiReasoning()).append("\n\n");
                }
                sb.append("Please reach out to the respective employees or trigger an automated notification ping.");
                aiResponse = sb.toString();
            }
        } else if (cleanMessage.contains("compliance score") || cleanMessage.contains("metrics")) {
            long total = alertRepository.count();
            long resolved = alertRepository.findByResolved(true).size();
            double complianceScore = total == 0 ? 100.0 : ((double) resolved / total) * 100.0;
            
            aiResponse = "### Enterprise Compliance Snapshot:\n\n" +
                    "- **Current Score**: **" + Math.round(complianceScore) + "%**\n" +
                    "- **Resolved Actions**: " + resolved + " alerts\n" +
                    "- **Outstanding Alerts**: " + (total - resolved) + " pending tasks\n\n" +
                    "**Analysis**: Action items under the 'COMPLIANCE' category (specifically 2FA violations) represent the primary drag on our safety index. Resolving pending 2FA compliance cases will raise the score to **92%**.";
        } else if (cleanMessage.contains("draft email") || cleanMessage.contains("email template") || cleanMessage.contains("write message")) {
            aiResponse = "Here is a professional, direct compliance notification email template for employees:\n\n" +
                    "```\nSubject: ACTION REQUIRED: Urgent Compliance Notification - [Alert Title]\n\n" +
                    "Dear [Employee Name],\n\n" +
                    "This is an automated notification from the SmartHR AI Compliance portal.\n\n" +
                    "We have flagged a pending requirement on your profile: \n" +
                    "\"[Alert Title] - [Alert Description]\"\n\n" +
                    "Our security policy mandates this item be resolved by [Deadline Date]. Please log in to your employee dashboard, head to 'Smart Alerts Workspace' and complete this action.\n\n" +
                    "If you face technical issues, please contact the HR Help Desk.\n\n" +
                    "Best regards,\nSmartHR Admin Compliance Team\n```";
        } else {
            if ("ADMIN".equals(user.getRole())) {
                aiResponse = "Hello! I am your **Smart Alerts AI Compliance Advisor**. I help you evaluate enterprise-wide risk metrics, flag security gaps, audit pending training tasks, and draft official notifications. You can ask me:\n\n" +
                        "1. *'What are the critical compliance risks right now?'*\n" +
                        "2. *'What is our current compliance score?'*\n" +
                        "3. *'Draft an email template for safety audit compliance.'*";
            } else {
                aiResponse = "Hello! I am your **Personal Compliance Assistant**. I help you review task deadlines, security policies, and pending action items on your profile. Ask me about details of any of your active notifications, and I will guide you on how to resolve them.";
            }
        }

        return ResponseEntity.ok(Collections.singletonMap("response", aiResponse));
    }
}
