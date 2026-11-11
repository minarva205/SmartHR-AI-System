package com.smarthr.backend.controller;

import com.smarthr.backend.model.Employee;
import com.smarthr.backend.model.ParsedResume;
import com.smarthr.backend.model.User;
import com.smarthr.backend.repository.EmployeeRepository;
import com.smarthr.backend.repository.ParsedResumeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resume-parser")
@CrossOrigin(origins = "*")
public class ResumeParserController {

    @Autowired
    private ParsedResumeRepository parsedResumeRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // Helper: Check admin session
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        return user != null && "ADMIN".equals(user.getRole());
    }

    // Helper: Get logged-in user
    private User getLoggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    // -------------------------------------------------
    // 1. GET /admin/records - All parsed resumes for HR
    // -------------------------------------------------
    @GetMapping("/admin/records")
    public ResponseEntity<?> getAdminRecords(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Access denied. Admins only.");
        }

        // Sync with Employee Directory (employees table) so names match the directory
        List<Employee> directoryEmployees = employeeRepository.findAll();

        for (Employee emp : directoryEmployees) {
            if (emp.getEmail() != null && !parsedResumeRepository.findByEmail(emp.getEmail()).isPresent()) {
                ParsedResume pr = seedFromEmployee(emp);
                parsedResumeRepository.save(pr);
            }
        }

        List<ParsedResume> allRecords = parsedResumeRepository.findAll();

        // If employee directory is also empty, fallback to demo records
        if (allRecords.isEmpty()) {
            parsedResumeRepository.saveAll(buildDemoRecords());
            allRecords = parsedResumeRepository.findAll();
        }

        // Build enriched response maps
        List<Map<String, Object>> result = new ArrayList<>();
        for (ParsedResume pr : allRecords) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", pr.getId());
            map.put("fullName", pr.getFullName());
            map.put("email", pr.getEmail());
            map.put("phone", pr.getPhone());
            map.put("currentRole", pr.getCurrentRole());
            map.put("education", pr.getEducation());
            map.put("skills", pr.getSkills());
            map.put("experienceYears", pr.getExperienceYears());
            map.put("resumeLink", pr.getResumeLink());
            map.put("aiSummary", pr.getAiSummary());
            map.put("fitRating", pr.getFitRating());
            map.put("skillMatchPercent", pr.getSkillMatchPercent());
            map.put("matchingJobTitle", pr.getMatchingJobTitle());
            map.put("parsedAt", pr.getParsedAt());
            result.add(map);
        }

        // Sort by skillMatchPercent descending
        result.sort((a, b) -> {
            int aVal = parsePercent((String) a.get("skillMatchPercent"));
            int bVal = parsePercent((String) b.get("skillMatchPercent"));
            return Integer.compare(bVal, aVal);
        });

        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------
    // 2. GET /employee/my-record - Employee's own parse
    // -------------------------------------------------
    @GetMapping("/employee/my-record")
    public ResponseEntity<?> getMyRecord(HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized.");
        }

        Optional<ParsedResume> existing = parsedResumeRepository
                .findTopByApplicantUserIdOrderByParsedAtDesc(user.getId());
        if (existing.isPresent()) {
            return ResponseEntity.ok(existing.get());
        }

        // Create baseline from user profile if no record exists
        ParsedResume pr = seedFromUser(user);
        parsedResumeRepository.save(pr);
        return ResponseEntity.ok(pr);
    }

    // -------------------------------------------------
    // 3. POST /upload - Parse & save resume
    // -------------------------------------------------
    @PostMapping("/upload")
    public ResponseEntity<?> uploadAndParse(
            @RequestParam("resumeFile") MultipartFile file,
            HttpSession session) {

        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized.");
        }

        // Save file
        String uploadDir = "src/main/resources/static/uploads/";
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists())
            dir.mkdirs();

        String fileName = "resume_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String filePath = "/uploads/" + fileName;

        try {
            file.transferTo(new java.io.File(dir.getAbsolutePath() + "/" + fileName));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("File upload failed: " + e.getMessage());
        }

        // Heuristic parse: deterministic seed based on user ID
        ParsedResume pr = parsedResumeRepository
                .findTopByApplicantUserIdOrderByParsedAtDesc(user.getId())
                .orElse(new ParsedResume());

        pr.setApplicantUserId(user.getId());
        pr.setFullName(user.getFullName() != null ? user.getFullName() : "Candidate");
        pr.setEmail(user.getEmail());
        pr.setPhone(user.getPhone() != null ? user.getPhone() : "+91-98765-43210");
        pr.setResumeLink(filePath);
        pr.setParsedAt(LocalDateTime.now());

        // Deterministic simulation based on user ID hash
        long seed = user.getId() != null ? user.getId() : 1L;
        enrichParsedResume(pr, seed, user.getFullName(), user.getDepartment(), user.getQualification());

        parsedResumeRepository.save(pr);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Resume parsed successfully.");
        response.put("record", pr);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------
    // 4. POST /admin/match-report - AI Screening Match
    // -------------------------------------------------
    @PostMapping("/admin/match-report")
    public ResponseEntity<?> generateMatchReport(@RequestBody Map<String, Long> payload, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).body("Access denied.");
        }

        Long resumeId = payload.get("resumeId");
        if (resumeId == null)
            return ResponseEntity.badRequest().body("resumeId required.");

        Optional<ParsedResume> opt = parsedResumeRepository.findById(resumeId);
        if (!opt.isPresent())
            return ResponseEntity.notFound().build();

        ParsedResume pr = opt.get();
        String matchJob = pr.getMatchingJobTitle() != null ? pr.getMatchingJobTitle() : "Software Engineer";
        String skillList = pr.getSkills() != null ? pr.getSkills() : "Java, Spring Boot";

        StringBuilder report = new StringBuilder();
        report.append("### AI Screening Report for ").append(pr.getFullName()).append("\n\n");
        report.append("**Best Match Position**: ").append(matchJob).append("\n\n");
        report.append("**AI Fit Rating**: ").append(pr.getFitRating()).append(" | **Skill Match**: ")
                .append(pr.getSkillMatchPercent()).append("\n\n");
        report.append("**Professional Summary**: ").append(pr.getAiSummary()).append("\n\n");

        report.append("#### ✅ Strengths\n");
        String[] skills = skillList.split(", ");
        for (String skill : skills) {
            report.append("- Proficient in **").append(skill.trim()).append("**\n");
        }

        report.append("\n#### ⚠️ Skill Gaps to Assess\n");
        String[] gaps = getSkillGaps(matchJob, skillList);
        for (String gap : gaps) {
            report.append("- ").append(gap).append("\n");
        }

        report.append("\n#### 💬 Suggested Interview Questions\n");
        report.append("- Describe a challenging project you led using ").append(skills[0].trim()).append(".\n");
        report.append("- How do you approach debugging in a production environment?\n");
        report.append("- Walk me through your experience with agile and cross-functional collaboration.\n");
        report.append("- What is your preferred architecture for scalable backend systems?\n");

        report.append("\n**Recommendation**: ")
                .append(pr.getFitRating().equals("Excellent") || pr.getFitRating().equals("Good")
                        ? "Shortlist for technical interview round."
                        : "Consider for culture-fit call before technical assessment.")
                .append("\n");

        return ResponseEntity.ok(Collections.singletonMap("report", report.toString()));
    }

    // -------------------------------------------------
    // 5. POST /ai-assistant - ResumeBot AI Chat
    // -------------------------------------------------
    @PostMapping("/ai-assistant")
    public ResponseEntity<?> aiAssistant(@RequestBody Map<String, String> payload, HttpSession session) {
        User user = getLoggedInUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthenticated.");
        }

        String message = payload.getOrDefault("message", "").toLowerCase().trim();
        String response;

        if ("ADMIN".equals(user.getRole())) {
            // HR Hiring Strategist Bot
            if (message.contains("best candidate") || message.contains("top candidate")
                    || message.contains("highest match")) {
                List<ParsedResume> all = parsedResumeRepository.findAll();
                if (all.isEmpty()) {
                    response = "No parsed resumes found yet. Ask candidates to upload their resumes first!";
                } else {
                    ParsedResume best = all.stream()
                            .max(Comparator.comparingInt(r -> parsePercent(r.getSkillMatchPercent())))
                            .orElse(all.get(0));
                    response = "The top-ranked candidate currently is **" + best.getFullName()
                            + "** with a skill match of **"
                            + best.getSkillMatchPercent() + "** for the role of **" + best.getMatchingJobTitle()
                            + "**.\n\n"
                            + "AI Summary: " + best.getAiSummary();
                }
            } else if (message.contains("shortlist") || message.contains("excellent")
                    || message.contains("top rated")) {
                List<ParsedResume> excellent = parsedResumeRepository.findAll().stream()
                        .filter(r -> "Excellent".equals(r.getFitRating()) || "Good".equals(r.getFitRating()))
                        .collect(Collectors.toList());
                if (excellent.isEmpty()) {
                    response = "No candidates with Excellent or Good ratings yet. Upload more resumes to populate the registry.";
                } else {
                    StringBuilder sb = new StringBuilder(
                            "Here are the **shortlisted candidates** with high fit ratings:\n\n");
                    for (ParsedResume r : excellent) {
                        sb.append("- **").append(r.getFullName()).append("** — ")
                                .append(r.getFitRating()).append(", ").append(r.getSkillMatchPercent())
                                .append(" match for ").append(r.getMatchingJobTitle()).append("\n");
                    }
                    response = sb.toString();
                }
            } else if (message.contains("interview") || message.contains("questions")) {
                response = "Here are some universal screening questions I recommend:\n\n"
                        + "- **Technical Depth**: Explain the difference between REST and GraphQL APIs.\n"
                        + "- **Problem Solving**: Walk me through how you would approach scaling a microservice under traffic spikes.\n"
                        + "- **Collaboration**: Describe a conflict with a team member and how you resolved it.\n"
                        + "- **Growth Mindset**: What is the last technical concept you learned independently?";
            } else if (message.contains("how many") || message.contains("count") || message.contains("total")) {
                long total = parsedResumeRepository.count();
                long excellent = parsedResumeRepository.findAll().stream()
                        .filter(r -> "Excellent".equals(r.getFitRating())).count();
                response = "Currently, there are **" + total + " parsed resumes** in the registry.\n"
                        + "- **Excellent Fit**: " + excellent + " candidates\n"
                        + "- **Other categories**: " + (total - excellent) + " candidates\n\n"
                        + "Generate an AI Screening Report for any candidate to get their detailed match breakdown.";
            } else {
                response = "Hello! I am **ResumeBot AI**, your hiring intelligence assistant. I can:\n\n"
                        + "- Identify the **top candidates** by skill match score\n"
                        + "- List **shortlisted profiles** rated Excellent or Good\n"
                        + "- Suggest **interview questions** for technical screening\n"
                        + "- Give a count of parsed candidates\n\n"
                        + "Try asking: *'Who is the best candidate?'* or *'Show me shortlisted profiles.'*";
            }
        } else {
            // Employee / Applicant Career Coach
            if (message.contains("improve") || message.contains("resume tips") || message.contains("better resume")) {
                response = "Here are AI-powered tips to improve your resume:\n\n"
                        + "- **Quantify achievements**: Instead of 'improved performance', write 'reduced API response time by 40%'.\n"
                        + "- **Tailor for the role**: Mirror keywords from the job description in your skills section.\n"
                        + "- **Skills section**: List technologies in order of proficiency. Group them (Languages, Frameworks, Cloud, Tools).\n"
                        + "- **Summary line**: Start with a one-sentence value proposition e.g., *'Full-stack developer with 5 years experience in Java and React.'*";
            } else if (message.contains("interview") || message.contains("prepare") || message.contains("mock")) {
                response = "Let's prep you for your interview! Here are practice questions:\n\n"
                        + "- *Tell me about yourself* — Lead with your strongest technical skill and a key project outcome.\n"
                        + "- *What is your biggest challenge?* — Pick a real scenario and explain what you learned.\n"
                        + "- *Why this role?* — Show you've researched the company and connect it to your career goals.\n"
                        + "- *Where do you see yourself in 5 years?* — Be honest about growth, tie it to the company's vision.";
            } else if (message.contains("skill") || message.contains("learn") || message.contains("gap")) {
                response = "Based on current market demand, here are skills worth adding to your profile:\n\n"
                        + "- **Cloud**: AWS / GCP certifications add significant weight to your CV.\n"
                        + "- **Containers**: Docker & Kubernetes are now standard for backend roles.\n"
                        + "- **AI/ML Basics**: Even non-ML engineers benefit from understanding embedding, prompting, and LLM integration.\n"
                        + "- **System Design**: Practice HLD/LLD interview questions on platforms like Exponent or ByteByteGo.";
            } else if (message.contains("hello") || message.contains("hi") || message.contains("help")) {
                response = "Hello! I am your **AI Career Coach**. I can help you:\n\n"
                        + "- Get **resume improvement tips** tailored to your profile\n"
                        + "- **Prepare for interviews** with mock questions\n"
                        + "- Identify **skill gaps** to fill for your target role\n\n"
                        + "Try: *'How can I improve my resume?'* or *'Give me mock interview questions.'*";
            } else {
                response = "Great question! As your AI career coach, I recommend staying consistent with learning and highlighting outcomes, not just responsibilities, on your resume. Would you like tips on resume writing, interview prep, or skills to learn next?";
            }
        }

        return ResponseEntity.ok(Collections.singletonMap("response", response));
    }

    // ============================
    // Private Helper Methods
    // ============================

    private int parsePercent(String pct) {
        if (pct == null)
            return 0;
        try {
            return Integer.parseInt(pct.replace("%", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String[] getSkillGaps(String jobTitle, String existingSkills) {
        List<String> all = new ArrayList<>();
        if (jobTitle.toLowerCase().contains("java") || jobTitle.toLowerCase().contains("backend")) {
            all.addAll(Arrays.asList("Kubernetes orchestration", "Distributed tracing (Jaeger/Zipkin)",
                    "Event-driven architecture (Kafka)"));
        } else if (jobTitle.toLowerCase().contains("frontend") || jobTitle.toLowerCase().contains("react")) {
            all.addAll(Arrays.asList("Micro-frontend architecture", "Performance profiling with Lighthouse",
                    "Web accessibility (WCAG standards)"));
        } else if (jobTitle.toLowerCase().contains("devops") || jobTitle.toLowerCase().contains("cloud")) {
            all.addAll(Arrays.asList("Terraform IaC experience", "Multi-region failover design",
                    "Observability stack (Grafana + Prometheus)"));
        } else {
            all.addAll(Arrays.asList("Confirm system design proficiency", "Cloud platform certification",
                    "Leadership & mentoring experience"));
        }
        return all.toArray(new String[0]);
    }

    private void enrichParsedResume(ParsedResume pr, long seed, String name, String dept, String qualification) {
        // Roles
        String[] roles = { "Software Engineer", "Senior Developer", "Full Stack Engineer", "Backend Specialist",
                "Tech Lead", "Java Developer", "React Developer", "DevOps Engineer" };
        String[] skills = {
                "Java, Spring Boot, Hibernate, MySQL, REST APIs, Docker",
                "React.js, TypeScript, Node.js, Redux, CSS3, Jest",
                "Python, Django, FastAPI, PostgreSQL, Redis, Celery",
                "AWS, Terraform, Docker, Kubernetes, Jenkins, Linux",
                "Java, Microservices, Kafka, Spring Cloud, Elasticsearch",
                "Angular, TypeScript, Spring Boot, Maven, CI/CD, Git"
        };
        String[] educations = {
                "B.Tech, Computer Science – IIT Delhi, 2018",
                "MCA – Pune University, 2019",
                "B.E, Information Technology – VTU, 2017",
                "B.Sc Computer Science – DU, 2020",
                "M.Tech, Software Engineering – NIT Trichy, 2018"
        };
        String[] ratings = { "Excellent", "Good", "Good", "Fair", "Excellent", "Good" };
        String[] jobs = { "Senior Java Developer", "React Frontend Engineer", "DevOps Engineer", "Full Stack Developer",
                "Backend API Specialist" };

        int idx = (int) (Math.abs(seed) % roles.length);
        int eduIdx = (int) (Math.abs(seed) % educations.length);
        int skillIdx = (int) (Math.abs(seed * 3) % skills.length);
        int ratingIdx = (int) (Math.abs(seed) % ratings.length);
        int jobIdx = (int) (Math.abs(seed * 7) % jobs.length);
        int expYears = (int) (2 + Math.abs(seed) % 10);
        int matchPct = (int) (55 + Math.abs(seed * 13) % 43);

        String role = roles[idx];
        String skillSet = skills[skillIdx];
        String education = educations[eduIdx];

        if (dept != null && !dept.trim().isEmpty()) {
            if (dept.toLowerCase().contains("front")) {
                role = "Frontend Developer";
                skillSet = skills[1];
            } else if (dept.toLowerCase().contains("devops")) {
                role = "DevOps Engineer";
                skillSet = skills[3];
            } else if (dept.toLowerCase().contains("python")) {
                role = "Python Developer";
                skillSet = skills[2];
            }
        }
        if (qualification != null && qualification.toLowerCase().contains("mtech")) {
            education = "M.Tech, Software Engineering – NIT, 2018";
        }

        pr.setCurrentRole(role);
        pr.setSkills(skillSet);
        pr.setEducation(education);
        pr.setExperienceYears(expYears);
        pr.setFitRating(ratings[ratingIdx]);
        pr.setSkillMatchPercent(matchPct + "%");
        pr.setMatchingJobTitle(jobs[jobIdx]);
        pr.setAiSummary("A results-driven **" + role + "** with " + expYears
                + " years of hands-on experience building robust, scalable systems. Proficient in "
                + skillSet.split(",")[0].trim()
                + " and related ecosystems. Demonstrated track record of delivering high-quality solutions in agile environments with cross-functional teams.");
    }

    private ParsedResume seedFromUser(User user) {
        ParsedResume pr = new ParsedResume();
        pr.setApplicantUserId(user.getId());
        pr.setFullName(user.getFullName() != null ? user.getFullName() : "Employee");
        pr.setEmail(user.getEmail());
        pr.setPhone(user.getPhone() != null ? user.getPhone() : "+91-9876543210");
        pr.setResumeLink(user.getResumeFile() != null ? user.getResumeFile() : "#");
        pr.setParsedAt(LocalDateTime.now());

        long seed = user.getId() != null ? user.getId() : 1L;
        enrichParsedResume(pr, seed, user.getFullName(), user.getDepartment(), user.getQualification());
        return pr;
    }

    private ParsedResume seedFromEmployee(Employee emp) {
        ParsedResume pr = new ParsedResume();
        pr.setFullName(emp.getFullName() != null ? emp.getFullName() : "Employee");
        pr.setEmail(emp.getEmail());
        pr.setPhone("+91-9876543210");
        pr.setResumeLink("#");
        pr.setParsedAt(LocalDateTime.now().minusDays((long) (Math.random() * 7)));

        long seed = emp.getFullName() != null ? Math.abs((long) emp.getFullName().hashCode())
                : (emp.getId() != null ? emp.getId() : 1L);
        enrichParsedResume(pr, seed, emp.getFullName(), emp.getDepartment(), emp.getDesignation());
        return pr;
    }

    private List<ParsedResume> buildDemoRecords() {
        String[][] demos = {
                { "Arjun Sharma", "arjun.sharma@smarthr.com", "+91-9812345678", "Senior Java Developer",
                        "B.Tech CSE – IIT Bombay, 2017", "Java, Spring Boot, Hibernate, Kafka, Docker, AWS", "7",
                        "Excellent", "92%", "Senior Java Developer" },
                { "Priya Nair", "priya.nair@smarthr.com", "+91-9823456789", "React Frontend Engineer",
                        "B.Tech IT – VTU, 2019", "React.js, TypeScript, Redux, Node.js, TailwindCSS", "5", "Good",
                        "85%", "React Frontend Engineer" },
                { "Rahul Verma", "rahul.verma@smarthr.com", "+91-9834567890", "DevOps Engineer",
                        "MCA – Pune University, 2018", "AWS, Docker, Kubernetes, Terraform, Jenkins, Linux", "6",
                        "Excellent", "89%", "DevOps Engineer" },
                { "Sneha Kulkarni", "sneha.kulkarni@smarthr.com", "+91-9845678901", "Full Stack Developer",
                        "B.E CSE – NIT Trichy, 2020", "Java, React.js, MySQL, Spring Boot, REST APIs, Git", "4", "Good",
                        "78%", "Full Stack Developer" },
                { "Mohammed Rizwan", "m.rizwan@smarthr.com", "+91-9856789012", "Python Developer",
                        "M.Tech Software – BITS Pilani, 2018", "Python, Django, FastAPI, PostgreSQL, Redis, Celery",
                        "6", "Good", "81%", "Backend API Specialist" },
                { "Anika Singh", "anika.singh@smarthr.com", "+91-9867890123", "Angular Developer", "B.Sc CS – DU, 2021",
                        "Angular, TypeScript, Spring Boot, Maven, CI/CD", "3", "Fair", "63%", "Frontend Developer" },
        };

        List<ParsedResume> list = new ArrayList<>();
        for (String[] d : demos) {
            ParsedResume pr = new ParsedResume();
            pr.setFullName(d[0]);
            pr.setEmail(d[1]);
            pr.setPhone(d[2]);
            pr.setCurrentRole(d[3]);
            pr.setEducation(d[4]);
            pr.setSkills(d[5]);
            pr.setExperienceYears(Integer.parseInt(d[6]));
            pr.setFitRating(d[7]);
            pr.setSkillMatchPercent(d[8]);
            pr.setMatchingJobTitle(d[9]);
            pr.setResumeLink("#");
            pr.setParsedAt(LocalDateTime.now().minusDays((long) (Math.random() * 14)));
            pr.setAiSummary("A highly skilled **" + d[3] + "** with " + d[6] + " years of experience specializing in "
                    + d[5].split(",")[0].trim()
                    + " and associated technologies. Proven ability to deliver high-quality solutions in fast-paced, agile environments.");
            list.add(pr);
        }
        return list;
    }
}
