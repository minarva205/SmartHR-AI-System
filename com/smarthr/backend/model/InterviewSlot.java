package com.smarthr.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_slots")
public class InterviewSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long requestedById; // Employee user ID
    private String candidateName;
    private String candidateEmail;

    private String interviewerName;
    private String interviewType; // Video, Phone, In-Person

    private String preferredDate;      // e.g. "2026-05-25"
    private String preferredTimeRange; // e.g. "Morning (9AM-12PM)"
    private String duration;           // "30 min", "45 min", "60 min"

    private String scheduledDate;      // AI confirmed date
    private String scheduledTime;      // AI confirmed time
    private String meetingLink;
    private String location;

    private String jobRole;
    private String department;
    private String notes;

    // AI-generated fields
    private String aiStatus;           // Optimal / Conflict / Rescheduled
    @Column(columnDefinition = "TEXT")
    private String aiMatchReason;
    @Column(columnDefinition = "TEXT")
    private String aiPreparationTips;
    private Integer aiConfidenceScore; // 0-100

    private String status; // Pending, Confirmed, Cancelled, Completed, Rescheduled

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() { return id; }

    public Long getRequestedById() { return requestedById; }
    public void setRequestedById(Long requestedById) { this.requestedById = requestedById; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }

    public String getInterviewerName() { return interviewerName; }
    public void setInterviewerName(String interviewerName) { this.interviewerName = interviewerName; }

    public String getInterviewType() { return interviewType; }
    public void setInterviewType(String interviewType) { this.interviewType = interviewType; }

    public String getPreferredDate() { return preferredDate; }
    public void setPreferredDate(String preferredDate) { this.preferredDate = preferredDate; }

    public String getPreferredTimeRange() { return preferredTimeRange; }
    public void setPreferredTimeRange(String preferredTimeRange) { this.preferredTimeRange = preferredTimeRange; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(String scheduledDate) { this.scheduledDate = scheduledDate; }

    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getJobRole() { return jobRole; }
    public void setJobRole(String jobRole) { this.jobRole = jobRole; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getAiStatus() { return aiStatus; }
    public void setAiStatus(String aiStatus) { this.aiStatus = aiStatus; }

    public String getAiMatchReason() { return aiMatchReason; }
    public void setAiMatchReason(String aiMatchReason) { this.aiMatchReason = aiMatchReason; }

    public String getAiPreparationTips() { return aiPreparationTips; }
    public void setAiPreparationTips(String aiPreparationTips) { this.aiPreparationTips = aiPreparationTips; }

    public Integer getAiConfidenceScore() { return aiConfidenceScore; }
    public void setAiConfidenceScore(Integer aiConfidenceScore) { this.aiConfidenceScore = aiConfidenceScore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
