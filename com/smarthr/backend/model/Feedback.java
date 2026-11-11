package com.smarthr.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;
    private String subject;
    private String message;
    private String category;
    private boolean isAnonymous;
    private String status = "Pending";
    
    private String sentiment;
    private Double sentimentScore;
    private String emotionalTone;

    @Column(columnDefinition = "TEXT")
    private String aiKeyTakeaways;

    @Column(columnDefinition = "TEXT")
    private String aiRecommendation;

    @Column(insertable = false, updatable = false)
    private LocalDateTime submittedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public Double getSentimentScore() {
        return sentimentScore;
    }

    public void setSentimentScore(Double sentimentScore) {
        this.sentimentScore = sentimentScore;
    }

    public String getEmotionalTone() {
        return emotionalTone;
    }

    public void setEmotionalTone(String emotionalTone) {
        this.emotionalTone = emotionalTone;
    }

    public String getAiKeyTakeaways() {
        return aiKeyTakeaways;
    }

    public void setAiKeyTakeaways(String aiKeyTakeaways) {
        this.aiKeyTakeaways = aiKeyTakeaways;
    }

    public String getAiRecommendation() {
        return aiRecommendation;
    }

    public void setAiRecommendation(String aiRecommendation) {
        this.aiRecommendation = aiRecommendation;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
