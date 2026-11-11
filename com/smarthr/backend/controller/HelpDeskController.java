package com.smarthr.backend.controller;

import com.smarthr.backend.model.HelpDeskTicket;
import com.smarthr.backend.model.User;
import com.smarthr.backend.repository.HelpDeskRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/helpdesk")
@CrossOrigin(origins = "*")
public class HelpDeskController {

    @Autowired
    private HelpDeskRepository ticketRepo;

    // 1. Get tickets based on Role (Admin gets all, Employee gets their own)
    @GetMapping("/tickets")
    public ResponseEntity<?> getTickets(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if ("ADMIN".equals(user.getRole()) || "HR".equals(user.getRole())) {
            return ResponseEntity.ok(ticketRepo.findAllByOrderByCreatedAtDesc());
        } else {
            return ResponseEntity.ok(ticketRepo.findByEmployeeIdOrderByCreatedAtDesc(user.getId()));
        }
    }

    // 2. Create a new ticket (Employee)
    @PostMapping("/tickets")
    public ResponseEntity<?> createTicket(@RequestBody HelpDeskTicket ticket, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ticket.setEmployeeId(user.getId());
        ticket.setStatus("Open"); // Default status
        ticketRepo.save(ticket);
        return ResponseEntity.ok(ticket);
    }

    // 3. Update Status (Open -> In Progress -> Resolved)
    @PutMapping("/tickets/{id}/status")
    public ResponseEntity<?> updateTicketStatus(@PathVariable Long id, @RequestParam String status,
            HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || (!"ADMIN".equals(user.getRole()) && !"HR".equals(user.getRole()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        HelpDeskTicket ticket = ticketRepo.findById(id).orElse(null);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }
        ticket.setStatus(status);
        ticketRepo.save(ticket);
        return ResponseEntity.ok(ticket);
    }
}
