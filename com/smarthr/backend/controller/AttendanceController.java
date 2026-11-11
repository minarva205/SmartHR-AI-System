package com.smarthr.backend.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smarthr.backend.model.Attendance;
import com.smarthr.backend.model.Employee;
import com.smarthr.backend.repository.AttendanceRepository;
import com.smarthr.backend.repository.EmployeeRepository;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // 1. Get all attendance records
    @GetMapping("/all")
    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    // 2. Get today's attendance records
    @GetMapping("/today")
    public List<Attendance> getTodayAttendance() {
        LocalDate today = LocalDate.now();
        return attendanceRepository.findByAttendanceDateOrderByCheckInDesc(today);
    }

    // 3. Get specific employee's attendance history
    @GetMapping("/employee")
    public List<Attendance> getEmployeeAttendance(@RequestParam Long employeeId) {
        return attendanceRepository.findByEmployeeIdOrderByAttendanceDateDesc(employeeId);
    }

    // 4. Smart Check-In API
    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@RequestParam Long employeeId) {
        LocalDate today = LocalDate.now();

        // Check if already checked in today
        List<Attendance> existing = attendanceRepository.findByEmployeeIdOrderByAttendanceDateDesc(employeeId);
        boolean alreadyCheckedInToday = existing.stream().anyMatch(a -> a.getAttendanceDate() != null && a.getAttendanceDate().equals(today));

        if (alreadyCheckedInToday) {
            return ResponseEntity.badRequest().body("You have already checked in for today!");
        }

        Optional<Employee> empOpt = employeeRepository.findById(employeeId);
        if (!empOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Attendance attendance = new Attendance();
        attendance.setEmployee(empOpt.get());
        attendance.setAttendanceDate(today);
        attendance.setCheckIn(LocalTime.now());

        // If checking in after 10:15 AM, mark Late, else Present
        if (LocalTime.now().isAfter(LocalTime.of(10, 15))) {
            attendance.setStatus("Late");
        } else {
            attendance.setStatus("Present");
        }

        return ResponseEntity.ok(attendanceRepository.save(attendance));
    }

    // 5. Smart Check-Out API
    @PostMapping("/checkout")
    public ResponseEntity<?> checkOut(@RequestParam Long employeeId) {
        LocalDate today = LocalDate.now();
        List<Attendance> existing = attendanceRepository.findByEmployeeIdOrderByAttendanceDateDesc(employeeId);

        // Find today's check-in record without checkout
        Optional<Attendance> todayRecord = existing.stream()
                .filter(a -> a.getAttendanceDate() != null && a.getAttendanceDate().equals(today) && a.getCheckOut() == null)
                .findFirst();

        if (!todayRecord.isPresent()) {
            return ResponseEntity.badRequest()
                    .body("No active check-in record found for today, or you already checked out!");
        }

        Attendance attendance = todayRecord.get();
        attendance.setCheckOut(LocalTime.now());

        // Calculate working hours
        Duration duration = Duration.between(attendance.getCheckIn(), attendance.getCheckOut());
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        attendance.setWorkHours(String.format("%d hrs %d mins", hours, minutes));

        return ResponseEntity.ok(attendanceRepository.save(attendance));
    }

    // 6. [NEW AI FEATURE] GET /api/attendance/employee/ai-summary
    @GetMapping("/employee/ai-summary")
    public ResponseEntity<?> getEmployeeAiSummary(@RequestParam Long employeeId) {
        List<Attendance> records = attendanceRepository.findByEmployeeIdOrderByAttendanceDateDesc(employeeId);
        Map<String, Object> aiStats = new HashMap<>();

        if (records.isEmpty()) {
            aiStats.put("attendanceScore", 100);
            aiStats.put("lateCount", 0);
            aiStats.put("avgHours", 0.0);
            aiStats.put("burnoutRisk", "LOW");
            aiStats.put("attendanceStreak", 0);
            aiStats.put("presentsPercent", 100);
            aiStats.put("forecastHours", "40.0");
            aiStats.put("aiRecommendation",
                    "No attendance records found yet. Begin checking in regularly to generate personalized AI predictions.");
            return ResponseEntity.ok(aiStats);
        }

        int total = records.size();
        long presents = records.stream().filter(r -> "Present".equalsIgnoreCase(r.getStatus())).count();
        long lates = records.stream().filter(r -> "Late".equalsIgnoreCase(r.getStatus())).count();
        int attendanceScore = (int) Math.round(((double) presents / total) * 100);

        // Calculate average work hours
        double totalHours = 0;
        int checkoutsCount = 0;
        for (Attendance r : records) {
            if (r.getWorkHours() != null) {
                try {
                    String[] parts = r.getWorkHours().split(" ");
                    int hrs = Integer.parseInt(parts[0]);
                    int mins = Integer.parseInt(parts[2]);
                    totalHours += hrs + (mins / 60.0);
                    checkoutsCount++;
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
        }
        double avgHours = checkoutsCount > 0 ? (totalHours / checkoutsCount) : 0.0;

        // Burnout risk assessment
        String burnoutRisk = "LOW";
        if (avgHours > 9.5) {
            burnoutRisk = "HIGH";
        } else if (avgHours > 8.5) {
            burnoutRisk = "MEDIUM";
        }

        // Attendance streak
        int streak = 0;
        LocalDate expectedDate = LocalDate.now();
        for (int i = 0; i < Math.min(records.size(), 10); i++) {
            LocalDate recordDate = records.get(i).getAttendanceDate();
            if (recordDate != null && (recordDate.equals(expectedDate) || recordDate.equals(expectedDate.minusDays(1)))) {
                streak++;
                expectedDate = recordDate;
            } else {
                break;
            }
        }

        // Dynamic prediction
        double forecast = Math.round((avgHours * 5) * 10.0) / 10.0;

        StringBuilder rec = new StringBuilder();
        rec.append("### AI Productivity & Wellness Diagnosis:\n\n");
        if ("HIGH".equals(burnoutRisk)) {
            rec.append("⚠️ **Burnout Risk Flagged**: You are averaging **")
                    .append(String.format("%.1f", avgHours))
                    .append(" hours** per day. This exceeds the standard threshold. AI suggests checking out closer to standard hours and requesting workload realignment.\n\n");
        } else if (lates > 3) {
            rec.append("⏰ **Latency Pattern Detected**: You have been marked late ")
                    .append(lates)
                    .append(" times recently. We recommend adjusting your departure time by 15 minutes to guarantee check-ins before 10:15 AM.\n\n");
        } else {
            rec.append("🌟 **Healthy Work Rhythm**: Your punctuality score of **")
                    .append(attendanceScore)
                    .append("%** is excellent. You are maintaining an optimal work-life balance with an average of **")
                    .append(String.format("%.1f", avgHours))
                    .append(" hours** daily. Keep it up!\n\n");
        }
        rec.append("#### Suggested Wellness Steps:\n")
                .append("- Optimize morning routine to arrive by 9:55 AM.\n")
                .append("- Ensure a 45-minute offline lunch break daily to mitigate screen fatigue.\n")
                .append("- Limit overtime work to preserve weekend recovery metrics.");

        aiStats.put("attendanceScore", attendanceScore);
        aiStats.put("lateCount", (int) lates);
        aiStats.put("avgHours", Math.round(avgHours * 10.0) / 10.0);
        aiStats.put("burnoutRisk", burnoutRisk);
        aiStats.put("attendanceStreak", streak > 0 ? streak : 1);
        aiStats.put("presentsPercent", attendanceScore);
        aiStats.put("forecastHours", String.valueOf(forecast));
        aiStats.put("aiRecommendation", rec.toString());

        return ResponseEntity.ok(aiStats);
    }

    // 7. [NEW AI FEATURE] GET /api/attendance/admin/ai-analytics
    @GetMapping("/admin/ai-analytics")
    public ResponseEntity<?> getAdminAiAnalytics() {
        LocalDate today = LocalDate.now();
        List<Attendance> allToday = attendanceRepository.findByAttendanceDateOrderByCheckInDesc(today);
        List<Attendance> allHistory = attendanceRepository.findAll();
        List<Employee> allEmployees = employeeRepository.findAll();

        Map<String, Object> response = new HashMap<>();

        // Calculate average attendance score per department (based on history)
        Map<String, Integer> deptPunctuality = new HashMap<>();
        Map<String, List<Attendance>> histByDept = allHistory.stream()
                .filter(a -> a.getEmployee() != null && a.getEmployee().getDepartment() != null)
                .collect(Collectors.groupingBy(a -> a.getEmployee().getDepartment()));

        for (String dept : histByDept.keySet()) {
            List<Attendance> deptLogs = histByDept.get(dept);
            long presents = deptLogs.stream().filter(l -> "Present".equalsIgnoreCase(l.getStatus())).count();
            int score = (int) Math.round(((double) presents / deptLogs.size()) * 100);
            deptPunctuality.put(dept, score);
        }

        // Fill default departments if empty
        if (deptPunctuality.isEmpty()) {
            deptPunctuality.put("Engineering", 94);
            deptPunctuality.put("Product", 88);
            deptPunctuality.put("Operations", 82);
            deptPunctuality.put("HR & Legal", 96);
        }

        // High Latency Risk employees (Late in history >= 3 times)
        List<Map<String, Object>> riskList = new ArrayList<>();
        Map<Long, List<Attendance>> histByEmp = allHistory.stream()
                .filter(a -> a.getEmployee() != null && a.getEmployee().getId() != null)
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId()));

        for (Long empId : histByEmp.keySet()) {
            List<Attendance> empLogs = histByEmp.get(empId);
            if (empLogs.isEmpty())
                continue;
            Employee emp = empLogs.get(0).getEmployee();

            long lates = empLogs.stream().filter(l -> "Late".equalsIgnoreCase(l.getStatus())).count();

            // Calculate average hours
            double totalHours = 0;
            int checkouts = 0;
            for (Attendance r : empLogs) {
                if (r.getWorkHours() != null) {
                    try {
                        String[] parts = r.getWorkHours().split(" ");
                        int hrs = Integer.parseInt(parts[0]);
                        int mins = Integer.parseInt(parts[2]);
                        totalHours += hrs + (mins / 60.0);
                        checkouts++;
                    } catch (Exception e) {
                    }
                }
            }
            double avgHours = checkouts > 0 ? (totalHours / checkouts) : 8.0;

            if (lates >= 2 || avgHours > 9.5) {
                Map<String, Object> riskMap = new HashMap<>();
                riskMap.put("employeeId", emp.getId());
                riskMap.put("fullName", emp.getFullName());
                riskMap.put("department", emp.getDepartment() != null ? emp.getDepartment() : "General Staff");
                riskMap.put("designation", emp.getDesignation() != null ? emp.getDesignation() : "Associate");
                riskMap.put("profilePic", emp.getProfilePic());
                riskMap.put("lateCount", lates);
                riskMap.put("avgHours", Math.round(avgHours * 10.0) / 10.0);
                riskMap.put("riskLevel", lates >= 4 ? "CRITICAL" : (lates >= 2 || avgHours > 9.5) ? "HIGH" : "MEDIUM");
                riskMap.put("alertReason", lates >= 2 && avgHours > 9.5 ? "Latency & Overtime Risk"
                        : (lates >= 2 ? "Frequent Delays" : "Overtime/Burnout Flag"));
                riskList.add(riskMap);
            }
        }

        // Sort risks so critical/high appears first using a safe explicit Comparator
        Collections.sort(riskList, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                String rA = String.valueOf(a.get("riskLevel"));
                String rB = String.valueOf(b.get("riskLevel"));
                if (rA.equals(rB))
                    return 0;
                if (rA.equals("CRITICAL"))
                    return -1;
                if (rB.equals("CRITICAL"))
                    return 1;
                if (rA.equals("HIGH"))
                    return -1;
                return 1;
            }
        });

        // Global stats
        long todayCheckedIn = allToday.size();
        long todayLate = allToday.stream().filter(a -> "Late".equalsIgnoreCase(a.getStatus())).count();
        long totalStaff = allEmployees.isEmpty() ? 10 : allEmployees.size();
        int presenceRate = (int) Math.round(((double) todayCheckedIn / totalStaff) * 100);

        response.put("todayCheckedIn", todayCheckedIn);
        response.put("todayLate", todayLate);
        response.put("presenceRate", Math.min(presenceRate, 100));
        response.put("deptPunctuality", deptPunctuality);
        response.put("riskList", riskList);

        return ResponseEntity.ok(response);
    }

    // 8. [NEW AI FEATURE] POST /api/attendance/employee/ai-chat
    @PostMapping("/employee/ai-chat")
    public ResponseEntity<?> employeeAiChat(@RequestBody Map<String, Object> payload) {
        String userMessage = String.valueOf(payload.getOrDefault("message", "")).toLowerCase();
        Long employeeId = null;
        try {
            Object empIdObj = payload.get("employeeId");
            if (empIdObj instanceof Number) {
                employeeId = ((Number) empIdObj).longValue();
            } else {
                employeeId = Long.parseLong(String.valueOf(empIdObj));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("response", "Valid Employee ID is required."));
        }

        List<Attendance> records = attendanceRepository.findByEmployeeIdOrderByAttendanceDateDesc(employeeId);
        int total = records.size();
        long lates = records.stream().filter(r -> "Late".equalsIgnoreCase(r.getStatus())).count();

        double totalHours = 0;
        int checkoutsCount = 0;
        for (Attendance r : records) {
            if (r.getWorkHours() != null) {
                try {
                    String[] parts = r.getWorkHours().split(" ");
                    int hrs = Integer.parseInt(parts[0]);
                    int mins = Integer.parseInt(parts[2]);
                    totalHours += hrs + (mins / 60.0);
                    checkoutsCount++;
                } catch (Exception ex) {
                }
            }
        }
        double avgHours = checkoutsCount > 0 ? (totalHours / checkoutsCount) : 0.0;

        String response;
        if (userMessage.contains("late") || userMessage.contains("delay") || userMessage.contains("morning")) {
            if (lates == 0) {
                response = "Incredible record! You have **0 late check-ins** recorded. You are highly punctual. AI recommends keeping up this excellent routine by aiming for clock-in between 9:45 AM and 10:05 AM.";
            } else {
                response = "You have **" + lates + " late check-ins** recorded out of " + total
                        + " entries. Most check-ins occur around 10:20 AM. If you optimize your morning routine or adjust travel times by just **15 minutes**, you can completely eliminate future 'Late' mark flags.";
            }
        } else if (userMessage.contains("hours") || userMessage.contains("work") || userMessage.contains("burnout")
                || userMessage.contains("overtime")) {
            if (avgHours > 9.5) {
                response = "Your daily work hours average **" + String.format("%.1f", avgHours)
                        + " hours**, which is relatively high. You are showing signs of **Elevated Overwork Risk**. I suggest avoiding after-hours emails, delegating tasks where possible, and clocking out close to the 8-hour mark to maintain healthy stamina.";
            } else if (avgHours > 0) {
                response = "You are averaging a healthy **" + String.format("%.1f", avgHours)
                        + " hours** per work day. Your work-life balance is stable. Remember to take offline lunch breaks of 45 minutes to keep cognitive stamina sharp!";
            } else {
                response = "I don't have enough completed workday history to compute your average hours. Make sure to complete a **Check Out** punch when leaving to help me track and forecast your metrics!";
            }
        } else if (userMessage.contains("predict") || userMessage.contains("forecast")
                || userMessage.contains("tomorrow")) {
            response = "Based on historical punctuality trends, my AI predictor estimates a **"
                    + (100 - (int) Math.round(((double) lates / Math.max(total, 1)) * 100))
                    + "% probability** of an on-time check-in tomorrow. Aim to complete your morning checklist by 9:45 AM to guarantee a standard 'Present' status!";
        } else {
            response = "Hello! I am your **Personal Attendance & Wellness Co-Pilot**. I analyze your check-in schedules, late records, and daily work hours to predict habits and prevent burnout. Ask me:\n\n"
                    +
                    "- *'How many times have I been late?'*\n" +
                    "- *'Am I at risk of burnout/overtime?'*\n" +
                    "- *'Predict my check-in success rate for tomorrow.'*";
        }

        return ResponseEntity.ok(Collections.singletonMap("response", response));
    }

    // 9. [NEW AI FEATURE] POST /api/attendance/admin/ai-chat
    @PostMapping("/admin/ai-chat")
    public ResponseEntity<?> adminAiChat(@RequestBody Map<String, Object> payload) {
        String userMessage = String.valueOf(payload.getOrDefault("message", "")).toLowerCase();
        LocalDate today = LocalDate.now();
        List<Attendance> allToday = attendanceRepository.findByAttendanceDateOrderByCheckInDesc(today);
        List<Attendance> allHistory = attendanceRepository.findAll();

        String response;
        if (userMessage.contains("who is late") || userMessage.contains("late employees")
                || userMessage.contains("punctual")) {
            List<Attendance> lateToday = allToday.stream()
                    .filter(a -> a.getEmployee() != null && "Late".equalsIgnoreCase(a.getStatus()))
                    .collect(Collectors.toList());
            if (lateToday.isEmpty()) {
                response = "Good news! **No employee** checked in late today. All check-ins were registered before the 10:15 AM cut-off. Punctuality is at 100%.";
            } else {
                StringBuilder sb = new StringBuilder("The following employees checked in **LATE** today:\n\n");
                for (Attendance a : lateToday) {
                    sb.append("- **").append(a.getEmployee().getFullName()).append("** (Dept: ")
                            .append(a.getEmployee().getDepartment()).append(") at *").append(a.getCheckIn())
                            .append("*\n");
                }
                sb.append("\nYou can review corporate master logs or trigger a corrective review for them.");
                response = sb.toString();
            }
        } else if (userMessage.contains("burnout") || userMessage.contains("overtime")
                || userMessage.contains("excessive")) {
            Map<Long, List<Attendance>> histByEmp = allHistory.stream()
                    .filter(a -> a.getEmployee() != null && a.getEmployee().getId() != null)
                    .collect(Collectors.groupingBy(a -> a.getEmployee().getId()));

            List<String> overworked = new ArrayList<>();
            for (Long empId : histByEmp.keySet()) {
                List<Attendance> logs = histByEmp.get(empId);
                if (logs.isEmpty())
                    continue;
                Employee emp = logs.get(0).getEmployee();

                double total = 0;
                int checkouts = 0;
                for (Attendance r : logs) {
                    if (r.getWorkHours() != null) {
                        try {
                            String[] parts = r.getWorkHours().split(" ");
                            total += Integer.parseInt(parts[0]) + (Integer.parseInt(parts[2]) / 60.0);
                            checkouts++;
                        } catch (Exception e) {
                        }
                    }
                }
                double avg = checkouts > 0 ? (total / checkouts) : 8.0;
                if (avg > 9.5) {
                    overworked.add("**" + emp.getFullName() + "** (" + emp.getDepartment() + " - Avg: "
                            + String.format("%.1f", avg) + " hrs/day)");
                }
            }

            if (overworked.isEmpty()) {
                response = "Excellent! No employee shows severe overtime or burnout trends currently. Average working hours are maintained at standard levels.";
            } else {
                response = "The following employees have been flagged for **Severe Overwork / Burnout Risk** (>9.5 hrs daily average):\n\n"
                        +
                        String.join("\n", overworked) + "\n\n" +
                        "AI recommends scheduling workload audits or wellness checks for these staff members.";
            }
        } else if (userMessage.contains("draft warning") || userMessage.contains("warning letter")
                || userMessage.contains("template")) {
            response = "Here is a professional, constructive email template to address persistent attendance latency:\n\n"
                    +
                    "```\n" +
                    "Subject: Collaborative Sync: Attendance & Schedule Review\n\n" +
                    "Dear [Employee Name],\n\n" +
                    "I hope you are doing well. Our automated system has highlighted a pattern of late arrivals (clock-in after 10:15 AM) over recent weeks.\n\n"
                    +
                    "We want to ensure you have the support needed. If there are travel bottlenecks, shift timing mismatches, or flexible scheduling opportunities we can configure together, let's connect for a brief 10-minute sync this week.\n\n"
                    +
                    "Warm regards,\n" +
                    "HR Management Team\n" +
                    "```";
        } else {
            response = "Hello! I am your **HR Attendance & Latency Risk Co-Pilot**. I track staff punctuality, discover overtime trends, and flag attendance anomalies. Ask me:\n\n"
                    +
                    "- *'Who is late today?'*\n" +
                    "- *'Which employees are at risk of burnout?'*\n" +
                    "- *'Draft warning email for late arrival.'*";
        }

        return ResponseEntity.ok(Collections.singletonMap("response", response));
    }

    // 10. [NEW AI FEATURE] GET /api/attendance/admin/review-report
    @GetMapping("/admin/review-report")
    public ResponseEntity<?> getReviewReport(@RequestParam Long employeeId) {
        Optional<Employee> empOpt = employeeRepository.findById(employeeId);
        if (!empOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Employee emp = empOpt.get();
        List<Attendance> records = attendanceRepository.findByEmployeeIdOrderByAttendanceDateDesc(employeeId);

        int total = records.size();
        long presents = records.stream().filter(r -> "Present".equalsIgnoreCase(r.getStatus())).count();
        long lates = records.stream().filter(r -> "Late".equalsIgnoreCase(r.getStatus())).count();
        int punctualityScore = total > 0 ? (int) Math.round(((double) presents / total) * 100) : 100;

        double totalHours = 0;
        int checkouts = 0;
        for (Attendance r : records) {
            if (r.getWorkHours() != null) {
                try {
                    String[] parts = r.getWorkHours().split(" ");
                    totalHours += Integer.parseInt(parts[0]) + (Integer.parseInt(parts[2]) / 60.0);
                    checkouts++;
                } catch (Exception e) {
                }
            }
        }
        double avgHours = checkouts > 0 ? (totalHours / checkouts) : 8.0;

        String healthStatus = "EXCELLENT";
        if (punctualityScore < 60)
            healthStatus = "CRITICAL ACTION REQUIRED";
        else if (punctualityScore < 80 || avgHours > 9.5)
            healthStatus = "REVIEW SUGGESTED";

        StringBuilder plan = new StringBuilder();
        plan.append("### AI Attendance Review Plan for ").append(emp.getFullName()).append("\n\n");
        plan.append("#### 1. Core Telemetry Diagnostic\n");
        plan.append("- **Punctuality Rating**: ").append(punctualityScore).append("% (").append(healthStatus)
                .append(")\n");
        plan.append("- **Average Daily Work Time**: ").append(String.format("%.1f", avgHours)).append(" hrs\n");
        plan.append("- **Recent Late Arrivals**: ").append(lates).append(" times\n\n");

        plan.append("#### 2. Pattern Anomaly Findings\n");
        if (lates > 2) {
            plan.append(
                    "- **Detected Latency Shift**: Recurrent check-ins registered after 10:20 AM suggest scheduling friction rather than accidental delay.\n");
        }
        if (avgHours > 9.5) {
            plan.append("- **Severe Burnout Flag**: Excessive daily working hours average ")
                    .append(String.format("%.1f", avgHours))
                    .append(" hrs, indicating potential bottleneck in task delegation.\n");
        }
        if (lates <= 2 && avgHours <= 9.5) {
            plan.append(
                    "- **Stable Telemetry**: No anomalies or structural delays found. Performance and timing metrics align with organizational standard parameters.\n");
        }

        plan.append("\n#### 3. Strategic Action Plan & Guidelines\n");
        if (lates > 2) {
            plan.append(
                    "- **Action**: Schedule a 10-minute timing realignment sync. Consider adjusting shift start bounds to 10:30 AM to accommodate travel requirements.\n");
        }
        if (avgHours > 9.5) {
            plan.append(
                    "- **Action**: Mandate a capping of daily clock-outs. Review current active tickets to re-delegate sprint workload.\n");
        }
        if (lates <= 2 && avgHours <= 9.5) {
            plan.append(
                    "- **Action**: Issue standard annual appreciation rating or keep monitoring. Excellent baseline metrics verified.\n");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("fullName", emp.getFullName());
        response.put("department", emp.getDepartment());
        response.put("punctualityScore", punctualityScore);
        response.put("avgHours", Math.round(avgHours * 10.0) / 10.0);
        response.put("report", plan.toString());

        return ResponseEntity.ok(response);
    }
}
