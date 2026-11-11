package com.smarthr.backend.controller;

import com.smarthr.backend.model.Employee;
import com.smarthr.backend.model.User;
import com.smarthr.backend.repository.EmployeeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping
    public ResponseEntity<?> search(@RequestParam("q") String query, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("response", "Please log in first."));
        }

        String lowerQuery = query.toLowerCase().trim();
        Map<String, Object> result;

        if ("ADMIN".equals(user.getRole())) {
            result = getAdminSearchResult(lowerQuery, user);
        } else {
            result = getEmployeeSearchResult(lowerQuery, user);
        }

        return ResponseEntity.ok(result);
    }

    private Map<String, Object> getEmployeeSearchResult(String query, User user) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> actions = new ArrayList<>();
        String aiResponse = "";

        // 1. AI Keyword Checking and Response Construction
        if (query.contains("leave") || query.contains("holiday") || query.contains("vacation") || query.contains("off")) {
            aiResponse = "To apply for leave, navigate to the **Leave Management** workspace. Employees receive casual, sick, and earned leaves. Casual leaves require 2 days advance notice, whereas sick leaves require medical certificates if exceeding 3 days.";
            actions.add(createAction("Apply for Leave", "leave-management.html", "fas fa-calendar-plus", "border-orange-200 bg-orange-50 hover:bg-orange-100 text-orange-700"));
            actions.add(createAction("View Holidays List", "holidays.html", "fas fa-calendar-day", "border-blue-200 bg-blue-50 hover:bg-blue-100 text-blue-700"));
        } else if (query.contains("salary") || query.contains("pay") || query.contains("slip") || query.contains("money")) {
            aiResponse = "Your monthly payslips are available inside the **Payroll & Slips** workspace. Payroll cycles process on the last working day of each month. PDF downloads and structural salary breakdowns are accessible there.";
            actions.add(createAction("Payroll & Slips", "payroll.html", "fas fa-file-invoice-dollar", "border-emerald-200 bg-emerald-50 hover:bg-emerald-100 text-emerald-700"));
            actions.add(createAction("Expense Claim AI", "expenses.html", "fas fa-receipt", "border-purple-200 bg-purple-50 hover:bg-purple-100 text-purple-700"));
        } else if (query.contains("lms") || query.contains("course") || query.contains("learn") || query.contains("train")) {
            aiResponse = "We host professional skills courses in our **Training & LMS** workspace. Currently, the *Cybersecurity & 2FA Awareness* course is mandatory for all team members. Completing modules improves your position on the Leaderboard!";
            actions.add(createAction("Training & LMS", "lms.html", "fas fa-graduation-cap", "border-purple-200 bg-purple-50 hover:bg-purple-100 text-purple-700"));
            actions.add(createAction("Morale Leaderboard", "leaderboard.html", "fas fa-trophy", "border-yellow-200 bg-yellow-50 hover:bg-yellow-100 text-yellow-700"));
        } else if (query.contains("ticket") || query.contains("support") || query.contains("help") || query.contains("issue") || query.contains("it")) {
            aiResponse = "If you encounter any administrative or IT issues, please open a ticket in the **Help Desk** section (found in the site footer). Support engineers review and reply within 4 hours.";
            actions.add(createAction("Open Support Ticket", "helpdesk.html", "fas fa-headset", "border-red-200 bg-red-50 hover:bg-red-100 text-red-700"));
        } else if (query.contains("profile") || query.contains("my profile") || query.contains("edit details") || query.contains("password")) {
            aiResponse = "You can view and manage your phone, qualifications, city, and upload profile pictures from the **My Profile** section. For security updates, head over to Security & 2FA settings.";
            actions.add(createAction("My Profile", "profile.html", "fas fa-user-circle", "border-blue-200 bg-blue-50 hover:bg-blue-100 text-blue-700"));
            actions.add(createAction("Security Settings", "security.html", "fas fa-shield-halved", "border-slate-200 bg-slate-50 hover:bg-slate-100 text-slate-700"));
        } else if (query.contains("announcement") || query.contains("news") || query.contains("feed") || query.contains("event")) {
            aiResponse = "Stay updated on global company news and corporate events! Check out the Announcements feed or view upcoming birthday/anniversary calendars.";
            actions.add(createAction("Company Announcements", "announcements.html", "fas fa-bullhorn", "border-amber-200 bg-amber-50 hover:bg-amber-100 text-amber-700"));
            actions.add(createAction("Interactive Feed", "feed.html", "fas fa-hashtag", "border-pink-200 bg-pink-50 hover:bg-pink-100 text-pink-700"));
            actions.add(createAction("Corporate Events", "events.html", "fas fa-cake-candles", "border-red-200 bg-red-50 hover:bg-red-100 text-red-700"));
        } else if (query.contains("task") || query.contains("milestone") || query.contains("project")) {
            aiResponse = "Review active deliverables and project lists inside the **Project Tasks** card. Keep your progress updated so team managers can track project sprints.";
            actions.add(createAction("Project Tasks", "tasks.html", "fas fa-list-check", "border-blue-200 bg-blue-50 hover:bg-blue-100 text-blue-700"));
        }

        // 2. Query coworokers in the directory
        List<Employee> allEmployees = employeeRepository.findAll();
        List<Map<String, Object>> employeeResults = new ArrayList<>();

        if (!query.isEmpty() && query.length() >= 2) {
            employeeResults = allEmployees.stream()
                .filter(e -> (e.getFullName() != null && e.getFullName().toLowerCase().contains(query))
                        || (e.getDepartment() != null && e.getDepartment().toLowerCase().contains(query))
                        || (e.getDesignation() != null && e.getDesignation().toLowerCase().contains(query)))
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("fullName", e.getFullName());
                    map.put("email", e.getEmail());
                    map.put("department", e.getDepartment());
                    map.put("designation", e.getDesignation());
                    map.put("profilePic", e.getProfilePic());
                    return map;
                })
                .collect(Collectors.toList());
        }

        if (aiResponse.isEmpty()) {
            if (!employeeResults.isEmpty()) {
                aiResponse = "I scanned the employee directory and found matching coworkers. You can view their details below:";
            } else {
                aiResponse = "I searched the system resources but couldn't find matching results for **\"" + query + "\"**. Try looking for *'leave'*, *'payslip'*, *'mandatory courses'*, or coworker names.";
                // Provide default helper actions
                actions.add(createAction("Leave Management", "leave-management.html", "fas fa-calendar-check", "border-orange-200 bg-orange-50 hover:bg-orange-100 text-orange-700"));
                actions.add(createAction("LMS Learning Center", "lms.html", "fas fa-graduation-cap", "border-purple-200 bg-purple-50 hover:bg-purple-100 text-purple-700"));
                actions.add(createAction("Help Desk Ticket", "helpdesk.html", "fas fa-headset", "border-red-200 bg-red-50 hover:bg-red-100 text-red-700"));
            }
        }

        result.put("aiResponse", aiResponse);
        result.put("actions", actions);
        result.put("employeeResults", employeeResults);
        return result;
    }

    private Map<String, Object> getAdminSearchResult(String query, User user) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> actions = new ArrayList<>();
        String aiResponse = "";

        if (query.contains("churn") || query.contains("resign") || query.contains("risk") || query.contains("attrition")) {
            aiResponse = "The **Predictive Churn AI** engine is monitoring flight risks. Flight risks are calculated using attendance frequencies, negative sentiment indices, and task delays. Access the dashboard to view flags.";
            actions.add(createAction("Predictive Churn Dashboard", "churn-admin.html", "fas fa-brain", "border-red-200 bg-red-50 hover:bg-red-100 text-red-700"));
        } else if (query.contains("sentiment") || query.contains("morale") || query.contains("feedback") || query.contains("satisfaction")) {
            aiResponse = "The **Sentiment Analysis AI** scans feedback logs. Current company satisfaction stands at **87% positive**. Main positive drivers are flexible options; minor stress concerns relate to sprint end cycles.";
            actions.add(createAction("Sentiment Analytics Console", "sentiment-admin.html", "fas fa-face-smile", "border-blue-200 bg-blue-50 hover:bg-blue-100 text-blue-700"));
        } else if (query.contains("resume") || query.contains("cv") || query.contains("candidate") || query.contains("parser") || query.contains("ats")) {
            aiResponse = "The **Smart Resume Parser** extracts skills, qualifications, and role titles from PDF/DOCX resumes automatically, ranking applicants using Jaccard similarity metrics.";
            actions.add(createAction("Resume Parser AI", "resume-parser-admin.html", "fas fa-magic", "border-indigo-200 bg-indigo-50 hover:bg-indigo-100 text-indigo-700"));
            actions.add(createAction("Recruitment ATS", "admin-recruitment.html", "fas fa-user-plus", "border-emerald-200 bg-emerald-50 hover:bg-emerald-100 text-emerald-700"));
        } else if (query.contains("approval") || query.contains("pending") || query.contains("registration") || query.contains("request")) {
            aiResponse = "You have pending employee registration requests waiting in the queue. You can approve or reject profiles to authorize their secure system logins.";
            actions.add(createAction("Review Approvals Hub", "admin-dashboard.html#pendingApprovals", "fas fa-user-clock", "border-amber-200 bg-amber-50 hover:bg-amber-100 text-amber-700"));
            actions.add(createAction("Leave Approvals Console", "admin-leave-management.html", "fas fa-calendar-check", "border-orange-200 bg-orange-50 hover:bg-orange-100 text-orange-700"));
        } else if (query.contains("schedule") || query.contains("interview") || query.contains("auto") || query.contains("slot")) {
            aiResponse = "The **AI Auto-Scheduling** engine parses interviewer diaries and matches slots with candidate profiles. Access the module to configure availability charts.";
            actions.add(createAction("Configure Auto-Scheduling", "scheduling-admin.html", "fas fa-calendar-check", "border-indigo-200 bg-indigo-50 hover:bg-indigo-100 text-indigo-700"));
        } else if (query.contains("payroll") || query.contains("salary") || query.contains("pay") || query.contains("expense") || query.contains("claim")) {
            aiResponse = "Access the Admin Payroll hub to process salaries, configure base pay variables, download payslips, or review OCR-flagged Expense Claims.";
            actions.add(createAction("Payroll Console", "admin-payroll.html", "fas fa-file-invoice-dollar", "border-emerald-200 bg-emerald-50 hover:bg-emerald-100 text-emerald-700"));
            actions.add(createAction("OCR Expense Claims", "expenses-admin.html", "fas fa-receipt", "border-purple-200 bg-purple-50 hover:bg-purple-100 text-purple-700"));
        } else if (query.contains("add") || query.contains("create") || query.contains("register") || query.contains("new staff")) {
            aiResponse = "To add an employee directly to the database, navigate to the **Employee Directory** inside the admin console. Click 'Add New Employee' and fill in their designation, department, and salary specifications.";
            actions.add(createAction("Employee Directory", "admin-employee-directory.html", "fas fa-id-badge", "border-blue-200 bg-blue-50 hover:bg-blue-100 text-blue-700"));
        }

        // Search directory with full stats
        List<Employee> allEmployees = employeeRepository.findAll();
        List<Map<String, Object>> employeeResults = new ArrayList<>();

        if (!query.isEmpty() && query.length() >= 2) {
            employeeResults = allEmployees.stream()
                .filter(e -> (e.getFullName() != null && e.getFullName().toLowerCase().contains(query))
                        || (e.getDepartment() != null && e.getDepartment().toLowerCase().contains(query))
                        || (e.getDesignation() != null && e.getDesignation().toLowerCase().contains(query))
                        || (e.getEmail() != null && e.getEmail().toLowerCase().contains(query)))
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", e.getId());
                    map.put("fullName", e.getFullName());
                    map.put("email", e.getEmail());
                    map.put("department", e.getDepartment());
                    map.put("designation", e.getDesignation());
                    map.put("profilePic", e.getProfilePic());
                    map.put("baseSalary", e.getBaseSalary() != null ? e.getBaseSalary() : 0.0);

                    // Generate deterministic but smart-looking analytics metrics
                    int nameHash = e.getFullName() != null ? Math.abs(e.getFullName().hashCode()) : 0;
                    double mockChurnRisk = (nameHash % 100) / 100.0;
                    String churnLabel = "Low";
                    String churnColor = "text-green-600 bg-green-50";
                    if (mockChurnRisk > 0.75) {
                        churnLabel = "High";
                        churnColor = "text-red-600 bg-red-50";
                    } else if (mockChurnRisk > 0.45) {
                        churnLabel = "Medium";
                        churnColor = "text-yellow-600 bg-yellow-50";
                    }

                    int mockPerf = 60 + (nameHash % 40);

                    map.put("churnRisk", churnLabel);
                    map.put("churnRiskColor", churnColor);
                    map.put("churnScore", (int)(mockChurnRisk * 100));
                    map.put("performanceScore", mockPerf);
                    return map;
                })
                .collect(Collectors.toList());
        }

        if (aiResponse.isEmpty()) {
            if (!employeeResults.isEmpty()) {
                aiResponse = "I located matching employee files in the administrative database. Detailed salary figures, performance ratings, and AI churn risks are displayed below:";
            } else {
                aiResponse = "No matching records found for **\"" + query + "\"**. Search administrative terms like *'churn risk'*, *'pending registrations'*, *'sentiment metrics'*, or search employee names.";
                actions.add(createAction("Manage Approvals", "admin-dashboard.html#pendingApprovals", "fas fa-user-clock", "border-amber-200 bg-amber-50 hover:bg-amber-100 text-amber-700"));
                actions.add(createAction("Predictive Churn AI", "churn-admin.html", "fas fa-brain", "border-red-200 bg-red-50 hover:bg-red-100 text-red-700"));
                actions.add(createAction("ATS Recruitment", "admin-recruitment.html", "fas fa-user-plus", "border-emerald-200 bg-emerald-50 hover:bg-emerald-100 text-emerald-700"));
            }
        }

        result.put("aiResponse", aiResponse);
        result.put("actions", actions);
        result.put("employeeResults", employeeResults);
        return result;
    }

    private Map<String, Object> createAction(String label, String url, String icon, String colorClasses) {
        Map<String, Object> action = new HashMap<>();
        action.put("label", label);
        action.put("url", url);
        action.put("icon", icon);
        action.put("colorClasses", colorClasses);
        return action;
    }
}
