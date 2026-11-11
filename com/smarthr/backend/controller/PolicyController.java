package com.smarthr.backend.controller;

import com.smarthr.backend.model.Policy;
import com.smarthr.backend.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@CrossOrigin(origins = "*")
public class PolicyController {

    @Autowired
    private PolicyRepository policyRepo;

    @GetMapping("/all")
    public List<Policy> getAllPolicies() {
        return policyRepo.findAll();
    }

    @PostMapping("/add")
    public Policy addPolicy(@RequestBody Policy policy) {
        return policyRepo.save(policy);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePolicy(@PathVariable Long id) {
        if (policyRepo.existsById(id)) {
            policyRepo.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
