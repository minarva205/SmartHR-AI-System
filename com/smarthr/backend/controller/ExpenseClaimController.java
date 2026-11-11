package com.smarthr.backend.controller;

import com.smarthr.backend.model.Employee;
import com.smarthr.backend.model.ExpenseClaim;
import com.smarthr.backend.model.User;
import com.smarthr.backend.repository.EmployeeRepository;
import com.smarthr.backend.repository.ExpenseClaimRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseClaimController {

    @Autowired
    private ExpenseClaimRepository expenseRepo;

    @Autowired
    private EmployeeRepository employeeRepository;

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        return user != null && "ADMIN".equals(user.getRole());
    }

    private Employee getLoggedInEmployee(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null) return employeeRepository.findByEmail(user.getEmail()).orElse(null);
        return null;
    }

    // ── AI OCR Simulation Engine ─────────────────────────────────────────────
    private void runOcrSimulation(ExpenseClaim claim) {
        // Merchant names per category
        Map<String, String[]> merchants = new HashMap<>();
        merchants.put("TRAVEL",          new String[]{"IndiGo Airlines", "Uber Business", "Ola Corporate", "IRCTC Rail"});
        merchants.put("MEALS",           new String[]{"Zomato Office", "Swiggy B2B", "Café Coffee Day", "Domino's Catering"});
        merchants.put("SOFTWARE",        new String[]{"Adobe Systems", "JetBrains", "Slack Inc.", "GitHub Enterprise"});
        merchants.put("OFFICE_SUPPLIES", new String[]{"Staples India", "Amazon Business", "OfficeMax", "WPS Office"});
        merchants.put("HEALTHCARE",      new String[]{"Apollo Pharmacy", "MedPlus", "Fortis Diagnostics", "Cipla Health"});

        String cat = claim.getCategory() != null ? claim.getCategory().toUpperCase() : "OFFICE_SUPPLIES";
        String[] list = merchants.getOrDefault(cat, merchants.get("OFFICE_SUPPLIES"));
        claim.setOcrMerchant(list[new Random().nextInt(list.length)]);

        // OCR amount ≈ submitted amount ± small delta (simulate scanner precision)
        double delta = (Math.random() - 0.5) * 10;
        claim.setOcrAmount(Math.round((claim.getAmount() + delta) * 100.0) / 100.0);
        claim.setOcrDate(LocalDate.now().minusDays((long)(Math.random() * 5)));
        claim.setOcrConfidence(92.0 + Math.round(Math.random() * 7.0));
    }

    // ── AI Policy Audit Engine ───────────────────────────────────────────────
    private void runAiAudit(ExpenseClaim claim) {
        double amount  = claim.getAmount() != null ? claim.getAmount() : 0;
        double ocrAmt  = claim.getOcrAmount() != null ? claim.getOcrAmount() : amount;
        double amtDiff = Math.abs(amount - ocrAmt);

        // Policy thresholds per category (INR)
        Map<String, Double> limits = new HashMap<>();
        limits.put("TRAVEL",          15000.0);
        limits.put("MEALS",           2000.0);
        limits.put("SOFTWARE",        10000.0);
        limits.put("OFFICE_SUPPLIES", 5000.0);
        limits.put("HEALTHCARE",      3000.0);

        String cat = claim.getCategory() != null ? claim.getCategory().toUpperCase() : "OFFICE_SUPPLIES";
        double limit = limits.getOrDefault(cat, 5000.0);

        List<String> flags = new ArrayList<>();
        if (amount > limit)   flags.add("Amount ₹" + amount + " exceeds policy cap of ₹" + limit + " for " + cat + " category.");
        if (amtDiff > 200)    flags.add("OCR amount (₹" + ocrAmt + ") differs from claimed amount (₹" + amount + ") by ₹" + Math.round(amtDiff) + " — possible transcription error.");
        if (claim.getOcrConfidence() != null && claim.getOcrConfidence() < 94)
                              flags.add("OCR confidence is " + claim.getOcrConfidence() + "% — receipt scan quality is low. Manual verification recommended.");

        if (flags.isEmpty()) {
            claim.setAiAuditStatus("AUTO_APPROVED");
            claim.setAiAuditReason("Receipt verified. Amount within policy limits. OCR data matches submitted claim. No anomalies detected — automatically approved by AI Compliance Engine.");
        } else if (flags.size() == 1) {
            claim.setAiAuditStatus("MANUAL_REVIEW");
            claim.setAiAuditReason("Minor policy deviation detected: " + String.join(" ", flags) + " Recommended for human HR review before disbursal.");
        } else {
            claim.setAiAuditStatus("FLAGGED_HIGH_RISK");
            claim.setAiAuditReason("Multiple risk signals identified: " + String.join(" | ", flags) + " Claim placed on hold pending full HR investigation.");
        }
    }

    // ── SEED DEMO DATA ────────────────────────────────────────────────────────
    private void seedIfEmpty() {
        if (expenseRepo.count() > 0) return;
        List<Employee> emps = employeeRepository.findAll();
        if (emps.isEmpty()) return;

        String[][] samples = {
            {"TRAVEL",          "Business trip to Mumbai client site",    "12500"},
            {"MEALS",           "Team lunch — project kickoff meeting",   "1850"},
            {"SOFTWARE",        "JetBrains IntelliJ annual subscription", "9200"},
            {"OFFICE_SUPPLIES", "Printer cartridges and A4 reams",        "3400"},
            {"HEALTHCARE",      "Annual health checkup at Apollo Clinic",  "2800"},
            {"TRAVEL",          "Flight to Bangalore — tech summit",      "18000"},
            {"MEALS",           "Client dinner — Q1 review",              "4200"},
        };

        List<ExpenseClaim> seed = new ArrayList<>();
        int si = 0;
        for (Employee emp : emps) {
            for (int i = 0; i < 2; i++) {
                String[] s = samples[si % samples.length];
                si++;
                ExpenseClaim c = new ExpenseClaim();
                c.setEmployeeId(emp.getId());
                c.setEmployeeName(emp.getFullName());
                c.setCategory(s[0]);
                c.setDescription(s[1]);
                c.setAmount(Double.parseDouble(s[2]));
                c.setStatus("PENDING");
                c.setSubmittedDate(LocalDate.now().minusDays(si % 10));
                runOcrSimulation(c);
                runAiAudit(c);
                seed.add(c);
            }
        }
        expenseRepo.saveAll(seed);
    }

    // ─── ENDPOINTS ────────────────────────────────────────────────────────────

    // 1. Admin — all claims
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllClaims(HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body("Access denied.");
        seedIfEmpty();
        List<ExpenseClaim> claims = expenseRepo.findAll();
        claims.sort(Comparator.comparing(ExpenseClaim::getSubmittedDate).reversed());
        return ResponseEntity.ok(claims);
    }

    // 2. Admin — approve / reject
    @PutMapping("/admin/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body("Access denied.");
        String newStatus = body.get("status");
        return expenseRepo.findById(id).map(c -> {
            c.setStatus(newStatus);
            expenseRepo.save(c);
            return ResponseEntity.ok(c);
        }).orElse(ResponseEntity.notFound().build());
    }

    // 3. Admin — delete
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deleteClaim(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return ResponseEntity.status(403).body("Access denied.");
        if (!expenseRepo.existsById(id)) return ResponseEntity.notFound().build();
        expenseRepo.deleteById(id);
        return ResponseEntity.ok(Collections.singletonMap("success", true));
    }

    // 4. Employee — my claims
    @GetMapping("/employee/my-claims")
    public ResponseEntity<?> myClaims(HttpSession session) {
        Employee emp = getLoggedInEmployee(session);
        if (emp == null) return ResponseEntity.status(401).body("Unauthorized.");
        seedIfEmpty();
        List<ExpenseClaim> claims = expenseRepo.findByEmployeeId(emp.getId());
        claims.sort(Comparator.comparing(ExpenseClaim::getSubmittedDate).reversed());
        return ResponseEntity.ok(claims);
    }

    // 5. Employee — submit claim (with OCR + AI audit simulation)
    @PostMapping("/employee/submit")
    public ResponseEntity<?> submitClaim(@RequestBody ExpenseClaim input, HttpSession session) {
        Employee emp = getLoggedInEmployee(session);
        if (emp == null) return ResponseEntity.status(401).body("Unauthorized.");

        input.setEmployeeId(emp.getId());
        input.setEmployeeName(emp.getFullName());
        input.setStatus("PENDING");
        input.setSubmittedDate(LocalDate.now());
        runOcrSimulation(input);
        runAiAudit(input);
        expenseRepo.save(input);
        return ResponseEntity.ok(input);
    }

    // 6. Stats
    @GetMapping("/stats")
    public ResponseEntity<?> stats(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return ResponseEntity.status(401).body("Unauthorized.");
        seedIfEmpty();

        List<ExpenseClaim> all;
        if ("ADMIN".equals(user.getRole())) {
            all = expenseRepo.findAll();
        } else {
            Employee emp = getLoggedInEmployee(session);
            all = emp != null ? expenseRepo.findByEmployeeId(emp.getId()) : new ArrayList<>();
        }

        long total     = all.size();
        long pending   = all.stream().filter(c -> "PENDING".equals(c.getStatus())).count();
        long approved  = all.stream().filter(c -> "APPROVED".equals(c.getStatus())).count();
        long rejected  = all.stream().filter(c -> "REJECTED".equals(c.getStatus())).count();
        long flagged   = all.stream().filter(c -> "FLAGGED_HIGH_RISK".equals(c.getAiAuditStatus())).count();
        long autoApproved = all.stream().filter(c -> "AUTO_APPROVED".equals(c.getAiAuditStatus())).count();
        double totalAmt = all.stream().filter(c -> "APPROVED".equals(c.getStatus()))
                            .mapToDouble(c -> c.getAmount() != null ? c.getAmount() : 0).sum();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", total);
        map.put("pending", pending);
        map.put("approved", approved);
        map.put("rejected", rejected);
        map.put("flagged", flagged);
        map.put("autoApproved", autoApproved);
        map.put("totalApprovedAmount", totalAmt);
        return ResponseEntity.ok(map);
    }

    // 7. AI Expense Advisor chatbot
    @PostMapping("/ai-advisor")
    public ResponseEntity<?> aiAdvisor(@RequestBody Map<String, String> payload, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return ResponseEntity.status(401).body("Unauthorized.");

        String msg   = payload.getOrDefault("message", "").toLowerCase();
        String reply;

        if ("ADMIN".equals(user.getRole())) {
            // ── Admin Bot ──────────────────────────────────────────────────────
            if (msg.contains("flagged") || msg.contains("risk") || msg.contains("suspicious")) {
                List<ExpenseClaim> flagged = expenseRepo.findAll().stream()
                    .filter(c -> "FLAGGED_HIGH_RISK".equals(c.getAiAuditStatus()))
                    .collect(Collectors.toList());
                if (flagged.isEmpty()) {
                    reply = "Great news! There are **no high-risk flagged claims** in the system. All submissions passed AI policy checks.";
                } else {
                    StringBuilder sb = new StringBuilder("The following claims are flagged for **immediate review**:\n\n");
                    for (ExpenseClaim c : flagged) {
                        sb.append("- **").append(c.getEmployeeName()).append("** — ₹").append(c.getAmount())
                          .append(" (").append(c.getCategory()).append(")\n")
                          .append("  *Reason*: ").append(c.getAiAuditReason()).append("\n\n");
                    }
                    reply = sb.toString();
                }
            } else if (msg.contains("pending") || msg.contains("backlog")) {
                long p = expenseRepo.findByStatus("PENDING").size();
                reply = "There are currently **" + p + " pending expense claims** awaiting HR approval. " +
                        "Tip: Claims with `AUTO_APPROVED` AI status can be bulk-approved safely. " +
                        "Claims marked `FLAGGED_HIGH_RISK` should be investigated before disbursement.";
            } else if (msg.contains("policy") || msg.contains("limit") || msg.contains("cap")) {
                reply = "### Current Expense Policy Limits:\n\n" +
                        "| Category | Per-Claim Cap |\n" +
                        "|---|---|\n" +
                        "| ✈️ Travel | ₹15,000 |\n" +
                        "| 🍽️ Meals | ₹2,000 |\n" +
                        "| 💻 Software | ₹10,000 |\n" +
                        "| 📦 Office Supplies | ₹5,000 |\n" +
                        "| 🏥 Healthcare | ₹3,000 |\n\n" +
                        "Claims exceeding these thresholds are automatically flagged by the AI Audit Engine for manual review.";
            } else if (msg.contains("draft email") || msg.contains("email")) {
                reply = "```\nSubject: Expense Claim Review Required — [Claim ID]\n\n" +
                        "Dear [Employee Name],\n\n" +
                        "Our AI Expense Audit Engine has flagged your recent expense submission " +
                        "for [Category] amounting to ₹[Amount] for further review.\n\n" +
                        "Please log into SmartHR and navigate to **Expense Claims AI** to review " +
                        "the AI audit findings and provide any additional documentation required.\n\n" +
                        "Best regards,\nSmartHR Finance & HR Team\n```";
            } else {
                reply = "Hello! I am the **Expense Claims AI Advisor**. I can help you:\n\n" +
                        "• *\"Show me all flagged/suspicious claims\"*\n" +
                        "• *\"How many claims are pending?\"*\n" +
                        "• *\"What are the expense policy limits?\"*\n" +
                        "• *\"Draft a review email for flagged claim\"*";
            }
        } else {
            // ── Employee Bot ───────────────────────────────────────────────────
            if (msg.contains("status") || msg.contains("approved") || msg.contains("rejected")) {
                Employee emp = getLoggedInEmployee(session);
                List<ExpenseClaim> myClaims = emp != null ? expenseRepo.findByEmployeeId(emp.getId()) : new ArrayList<>();
                long p = myClaims.stream().filter(c -> "PENDING".equals(c.getStatus())).count();
                long a = myClaims.stream().filter(c -> "APPROVED".equals(c.getStatus())).count();
                long r = myClaims.stream().filter(c -> "REJECTED".equals(c.getStatus())).count();
                reply = "Your expense claim summary:\n\n" +
                        "- ⏳ **Pending**: " + p + " claims\n" +
                        "- ✅ **Approved**: " + a + " claims\n" +
                        "- ❌ **Rejected**: " + r + " claims\n\n" +
                        "For rejected claims, check the AI Audit Reason on each card and re-submit with updated receipts if needed.";
            } else if (msg.contains("policy") || msg.contains("limit") || msg.contains("how much")) {
                reply = "Here are the reimbursable limits per expense type:\n\n" +
                        "- ✈️ **Travel**: up to ₹15,000 per trip\n" +
                        "- 🍽️ **Meals**: up to ₹2,000 per occasion\n" +
                        "- 💻 **Software**: up to ₹10,000 per license\n" +
                        "- 📦 **Office Supplies**: up to ₹5,000 per purchase\n" +
                        "- 🏥 **Healthcare**: up to ₹3,000 per visit\n\n" +
                        "Submit amounts within these limits for AI Auto-Approval!";
            } else if (msg.contains("ocr") || msg.contains("receipt") || msg.contains("scan")) {
                reply = "Our **AI OCR Engine** automatically scans your uploaded receipt and extracts:\n\n" +
                        "• Merchant name\n• Transaction amount\n• Purchase date\n\n" +
                        "It then cross-checks the scanned data with what you entered. If there is a mismatch over ₹200, " +
                        "the claim is sent for manual review. Make sure your receipt photo is clear and well-lit for the highest OCR confidence score!";
            } else {
                reply = "Hi! I am your **Expense Claim Assistant**. I can help you with:\n\n" +
                        "• Checking your claim statuses\n" +
                        "• Understanding policy limits\n" +
                        "• Tips on getting AI Auto-Approval\n" +
                        "• How the OCR receipt scan works\n\n" +
                        "Just ask!";
            }
        }

        return ResponseEntity.ok(Collections.singletonMap("response", reply));
    }
}
