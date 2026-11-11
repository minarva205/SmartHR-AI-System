package com.smarthr.backend.controller;

import java.util.Optional;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smarthr.backend.model.User;
import com.smarthr.backend.repository.UserRepository;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "resumeUpload", required = false) MultipartFile resumeUpload,
            RedirectAttributes redirectAttributes) {

        String uploadDir = "src/main/resources/static/uploads/";
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists())
            dir.mkdirs();

        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                String fileName = "profile_" + System.currentTimeMillis() + "_" + profileImage.getOriginalFilename();
                profileImage.transferTo(new java.io.File(dir.getAbsolutePath() + "/" + fileName));
                user.setProfilePic("/uploads/" + fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (resumeUpload != null && !resumeUpload.isEmpty()) {
            try {
                String fileName = "resume_" + System.currentTimeMillis() + "_" + resumeUpload.getOriginalFilename();
                resumeUpload.transferTo(new java.io.File(dir.getAbsolutePath() + "/" + fileName));
                user.setResumeFile("/uploads/" + fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String pwd = user.getPassword();
        if (pwd == null || pwd.length() < 8 || !pwd.matches(".*[A-Z].*") || !pwd.matches(".*[a-z].*")
                || !pwd.matches(".*[0-9].*") || !pwd.matches(".*[@$!%*?&].*")) {
            redirectAttributes.addAttribute("error", "weak_password");
            return "redirect:/signup.html";
        }

        if (user.getRole() == null) {
            user.setRole("EMPLOYEE");
        }

        if ("ADMIN".equals(user.getRole())) {
            user.setStatus("APPROVED"); // Admin auto-approved for simplicity
            redirectAttributes.addAttribute("success", "admin_registered");
        } else {
            // Require HR approval for new employees
            user.setStatus("PENDING");
            redirectAttributes.addAttribute("success", "employee_pending");
        }

        userRepository.save(user);
        return "redirect:/login.html";
    }

    @PostMapping("/api/login")
    public String loginUser(@RequestParam String email, @RequestParam String password, HttpSession session) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            User user = userOpt.get();
            if ("EMPLOYEE".equals(user.getRole()) && !"APPROVED".equals(user.getStatus())) {
                return "redirect:/login.html?error=pending_approval";
            }
            session.setAttribute("loggedInUser", user);

            if ("ADMIN".equals(user.getRole())) {
                return "redirect:/admin-dashboard.html";
            }
            return "redirect:/index.html";
        } else {
            return "redirect:/login.html?error=true";
        }
    }

    @GetMapping("/api/current-user")
    @ResponseBody
    public ResponseEntity<User> getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null) {
            // Re-fetch from DB to get latest status if needed
            Long userId = user.getId();
            return ResponseEntity.ok(userRepository.findById(userId).orElse(user));
        }
        return ResponseEntity.status(401).build();
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login.html";
    }

    @GetMapping("/api/admin/pending-users")
    @ResponseBody
    public ResponseEntity<?> getPendingUsers(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        java.util.List<User> pendingUsers = userRepository.findAll().stream()
                .filter(u -> "PENDING".equals(u.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(pendingUsers);
    }

    @GetMapping("/api/users/all")
    @ResponseBody
    public ResponseEntity<?> getAllApprovedUsers() {
        // Return only approved users for the directory
        java.util.List<User> approvedUsers = userRepository.findAll().stream()
                .filter(u -> "APPROVED".equals(u.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(approvedUsers);
    }

    @PostMapping("/api/admin/approve-user")
    @ResponseBody
    public ResponseEntity<?> approveUser(@RequestParam Long userId,
            @RequestParam String status, HttpSession session) {
        User adminUser = (User) session.getAttribute("loggedInUser");
        if (adminUser == null || !"ADMIN".equals(adminUser.getRole())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if ("APPROVED".equals(status) || "REJECTED".equals(status)) {
                user.setStatus(status);
                userRepository.save(user);
                return ResponseEntity.ok("User status updated to " + status);
            }
            return ResponseEntity.badRequest().body("Invalid status");
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/api/forgot-password")
    @ResponseBody
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String tempPassword = "SmartHR@" + (int) (Math.random() * 10000);
            user.setPassword(tempPassword);
            userRepository.save(user);
            return ResponseEntity.ok("Success! Since email isn't configured, your new temporary password is: "
                    + tempPassword + " (Please copy and login)");
        }
        return ResponseEntity.ok("No account found with this email.");
    }
}
