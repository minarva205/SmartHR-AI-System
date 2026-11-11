package com.smarthr.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parsed_resumes")
public class ParsedResume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long applicantUserId; // links to User or Applicant (by email/id)
    private String fullName;
    private String email;
    private String phone;
    private String currentRole;
    private String education;

    @Column(length = 2000)
    private String skills; // comma-separated list

    private Integer experienceYears;
    private String resumeLink; // external link or /uploads/ path

    @Column(length = 3000)
    private String aiSummary; // AI-generated bio

    private String fitRating;           // "Excellent", "Good", "Fair", "Needs Review"
    private String skillMatchPercent;   // e.g. "87%"
    private String matchingJobTitle;    // best matching open position
    private LocalDateTime parsedAt;

    @PrePersist
    public void prePersist() {
        if (parsedAt == null) {
            parsedAt = LocalDateTime.now();
        }
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicantUserId() { return applicantUserId; }
    public void setApplicantUserId(Long applicantUserId) { this.applicantUserId = applicantUserId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCurrentRole() { return currentRole; }
    public void setCurrentRole(String currentRole) { this.currentRole = currentRole; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public String getResumeLink() { return resumeLink; }
    public void setResumeLink(String resumeLink) { this.resumeLink = resumeLink; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getFitRating() { return fitRating; }
    public void setFitRating(String fitRating) { this.fitRating = fitRating; }

    public String getSkillMatchPercent() { return skillMatchPercent; }
    public void setSkillMatchPercent(String skillMatchPercent) { this.skillMatchPercent = skillMatchPercent; }

    public String getMatchingJobTitle() { return matchingJobTitle; }
    public void setMatchingJobTitle(String matchingJobTitle) { this.matchingJobTitle = matchingJobTitle; }

    public LocalDateTime getParsedAt() { return parsedAt; }
    public void setParsedAt(LocalDateTime parsedAt) { this.parsedAt = parsedAt; }
}
