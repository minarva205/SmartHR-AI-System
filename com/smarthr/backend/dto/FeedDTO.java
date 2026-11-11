package com.smarthr.backend.dto;

import java.time.LocalDateTime;

public class FeedDTO {
    private Long id;
    private String fullName;
    private String designation;
    private String content;
    private int likesCount;
    private LocalDateTime createdAt;

    public FeedDTO(Long id, String fullName, String designation, String content, int likesCount,
            LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.designation = designation;
        this.content = content;
        this.likesCount = likesCount;
        this.createdAt = createdAt;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDesignation() {
        return designation;
    }

    public String getContent() {
        return content;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
