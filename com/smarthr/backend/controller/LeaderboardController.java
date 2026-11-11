package com.smarthr.backend.controller;

import com.smarthr.backend.model.Review;
import com.smarthr.backend.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    @Autowired
    private ReviewRepository reviewRepo;

    @GetMapping("/top")
    public List<Map<String, Object>> getTopPerformers() {
        List<Review> reviews = reviewRepo.findAll();
        Set<Long> seenEmployees = new HashSet<>();
        
        return reviews.stream()
            .filter(r -> r.getEmployee() != null)
            .sorted((r1, r2) -> Double.compare(r2.getOverallScore(), r1.getOverallScore()))
            .filter(r -> seenEmployees.add(r.getEmployee().getId()))
            .map(r -> {
                Map<String, Object> map = new HashMap<>();
                map.put("fullName", r.getEmployee().getFullName());
                map.put("designation", r.getEmployee().getDesignation());
                map.put("overallScore", r.getOverallScore());
                return map;
            })
            .collect(Collectors.toList());
    }
}
