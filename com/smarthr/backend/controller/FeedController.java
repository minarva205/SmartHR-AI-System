package com.smarthr.backend.controller;

import com.smarthr.backend.model.Feed;
import com.smarthr.backend.dto.FeedDTO;
import com.smarthr.backend.repository.FeedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feeds")
@CrossOrigin(origins = "*") // Frontend connect karne ke liye
public class FeedController {

    @Autowired
    private FeedRepository feedRepo;

    // Sabhi posts fetch karna (Employee Details ke sath)
    @GetMapping("/all")
    public List<FeedDTO> getAllFeeds() {
        return feedRepo.getAllFeedsWithEmployeeDetails();
    }

    // Naya post create karna
    @PostMapping("/post")
    public Feed createPost(@RequestBody Feed feed) {
        return feedRepo.save(feed);
    }

    // Like functionality
    @PostMapping("/like/{id}")
    public void likePost(@PathVariable Long id) {
        Feed feed = feedRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        feed.setLikesCount(feed.getLikesCount() + 1);
        feedRepo.save(feed);
    }

    // Delete post mapping (for moderation)
    @DeleteMapping("/{id}")
    public org.springframework.http.ResponseEntity<?> deletePost(@PathVariable Long id) {
        if (feedRepo.existsById(id)) {
            feedRepo.deleteById(id);
            return org.springframework.http.ResponseEntity.ok().build();
        }
        return org.springframework.http.ResponseEntity.notFound().build();
    }
}