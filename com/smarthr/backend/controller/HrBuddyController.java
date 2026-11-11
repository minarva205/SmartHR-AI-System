package com.smarthr.backend.controller;

import com.smarthr.backend.model.Employee;
import com.smarthr.backend.model.User;
import com.smarthr.backend.repository.EmployeeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/hr-buddy")
@CrossOrigin(origins = "*")
public class HrBuddyController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> payload, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("response", "Please log in first."));
        }

        String message = payload.getOrDefault("message", "").toLowerCase();
        String response;

        if ("ADMIN".equals(user.getRole())) {
            response = getAdminResponse(message, user);
        } else {
            response = getEmployeeResponse(message, user);
        }

        return ResponseEntity.ok(Collections.singletonMap("response", response));
    }

    private String getEmployeeResponse(String message, User user) {
        Employee emp = employeeRepository.findByEmail(user.getEmail()).orElse(null);
        String name = (emp != null) ? emp.getFullName() : user.getFullName();

        if (message.contains("leave") || message.contains("holiday") || message.contains("vacation")) {
            return "Hi " + name + "! To apply for leave, navigate to the **Leave Management** workspace from your dashboard. Under company policy, casual leave requires a 2-day advance notice, and sick leave needs medical certification if it exceeds 3 consecutive days.";
        } else if (message.contains("training") || message.contains("lms") || message.contains("course") || message.contains("learn")) {
            return "Hello " + name + "! You can access your assigned courses in the **Learning Management System (LMS)**. Currently, the *Cybersecurity & 2FA Awareness* course is mandatory for all team members. Completing courses boosts your rating on the company leaderboard!";
        } else if (message.contains("payroll") || message.contains("salary") || message.contains("payslip")) {
            return "Your monthly payslips are available in the **Payroll & Compensation** section. Salaries are disbursed on the last working day of each month. If you see any discrepancies, please submit a query via the Help Desk.";
        } else if (message.contains("help") || message.contains("ticket") || message.contains("support")) {
            return "If you're facing technical or HR issues, you can submit a support ticket in the **Help Desk** section (available in the page footer). Our support team usually replies within 4 hours.";
        } else if (message.contains("hello") || message.contains("hi") || message.contains("hey")) {
            return "Hey " + name + "! 😊 I'm your **SmartHR AI Buddy**. How can I help you today? You can ask me about leaves, payroll, mandatory LMS courses, or raising help desk support tickets.";
        } else {
            return "I hear you, " + name + "! As your virtual buddy, I recommend checking out the respective workspace cards on your dashboard for self-service actions. Let me know if you have specific policy questions!";
        }
    }

    private String getAdminResponse(String message, User user) {
        String name = user.getFullName() != null ? user.getFullName() : "Administrator";

        if (message.contains("draft") || message.contains("announcement") || message.contains("email")) {
            return "Certainly, " + name + "! Here is a template for a system announcement:\n\n" +
                   "```\n" +
                   "Subject: Scheduled System Upgrades & Compliance Audit\n\n" +
                   "Dear Team,\n\n" +
                   "Please note that the HR portal will undergo scheduled updates this Friday at 10 PM IST. " +
                   "Additionally, we request all employees to ensure their 2-factor authentication is active.\n\n" +
                   "Best regards,\nSmartHR Admin Team\n" +
                   "```\n\nYou can post this in the Announcements hub.";
        } else if (message.contains("sentiment") || message.contains("feedback") || message.contains("morale")) {
            return "Based on the latest feedback logs from the **Sentiment Analysis AI** engine, company morale is currently at **87% positive**. Main positive drivers are flexibility and learning opportunities; minor concerns relate to task distribution during peak sprint cycles.";
        } else if (message.contains("performance") || message.contains("leaderboard") || message.contains("top")) {
            long totalEmps = employeeRepository.count();
            return "We currently have **" + totalEmps + " active employees** in the directory. " +
                   "According to the LMS Leaderboard logs, the top department by learning hours is **Engineering**, followed closely by **Product Development**.";
        } else if (message.contains("pending") || message.contains("claim") || message.contains("approval")) {
            return "Admin alerts show pending actions across Expense Claims and Auto-Scheduling. Please navigate to the **Expense Claims AI** dashboard to review flagged policy submissions or view the **AI Auto-Scheduling** workspace to manage recruitment pipelines.";
        } else if (message.contains("hello") || message.contains("hi") || message.contains("hey")) {
            return "Hello, Admin " + name + "! 🛡️ I am your **HR Administrative Co-Pilot**. I can help you draft system announcements, retrieve company sentiment indices, overview directory stats, or locate pending approvals.";
        } else {
            return "Understood. As the HR Admin Co-pilot, I can assist you with system metrics, draft notifications, or provide summaries of active workflows. How would you like to proceed?";
        }
    }
}
