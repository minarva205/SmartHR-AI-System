package com.smarthr.backend.controller;

import com.smarthr.backend.model.User;
import com.smarthr.backend.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/security")
public class SecurityController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestParam String oldPassword,
                                            @RequestParam String newPassword,
                                            HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Optional<User> userOpt = userRepository.findById(loggedInUser.getId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.getPassword().equals(oldPassword)) {
                return ResponseEntity.badRequest().body("Incorrect old password");
            }
            if (newPassword == null || newPassword.length() < 8) {
                return ResponseEntity.badRequest().body("Password must be at least 8 characters");
            }
            user.setPassword(newPassword);
            userRepository.save(user);
            // Update session
            session.setAttribute("loggedInUser", user);
            return ResponseEntity.ok("Password changed successfully");
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/toggle-2fa")
    public ResponseEntity<?> toggle2FA(@RequestParam boolean enable, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Optional<User> userOpt = userRepository.findById(loggedInUser.getId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setTwoFactorEnabled(enable);
            userRepository.save(user);
            session.setAttribute("loggedInUser", user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", enable ? "2FA Enabled" : "2FA Disabled");
            response.put("twoFactorEnabled", enable);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/status")
    public ResponseEntity<?> getSecurityStatus(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        Optional<User> userOpt = userRepository.findById(loggedInUser.getId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("twoFactorEnabled", user.isTwoFactorEnabled());
            response.put("passwordLastChanged", "Today"); // Placeholder
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }
}
