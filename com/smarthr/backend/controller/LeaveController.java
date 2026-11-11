package com.smarthr.backend.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smarthr.backend.model.LeaveRequest;
import com.smarthr.backend.repository.LeaveRepository;

@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(origins = "*")
public class LeaveController {

    @Autowired
    private LeaveRepository leaveRepository;

    // 1. Get all leaves (Admin master view ke liye)
    @GetMapping("/all")
    public List<LeaveRequest> getAllLeaves() {
        return leaveRepository.findAll();
    }

    // 2. Get leaves for a specific employee (Employee dashboard self history
    // tracking)
    @GetMapping("/employee/{empId}")
    public List<LeaveRequest> getEmployeeLeaves(@PathVariable Long empId) {
        return leaveRepository.findByEmployeeId(empId);
    }

    // 3. Apply for leave (Dono side entry access handle standard data model format)
    @PostMapping("/apply")
    public LeaveRequest applyLeave(@RequestBody LeaveRequest leaveRequest) {
        return leaveRepository.save(leaveRequest);
    }

    // 4. Update Status (Admin Approve / Reject action command trigger update)
    @PostMapping("/{id}/status")
    public ResponseEntity<LeaveRequest> updateStatus(@PathVariable Long id,
            @RequestBody java.util.Map<String, String> statusMap) {
        return leaveRepository.findById(id).map(leave -> {
            leave.setStatus(statusMap.get("status"));
            LeaveRequest updated = leaveRepository.save(leave);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }
}