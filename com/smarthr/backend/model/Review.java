package com.smarthr.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "performance_reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private LocalDate reviewDate;
    private Integer technicalRating;
    private Integer softSkillsRating;
    private Integer attendanceRating;
    private Double overallScore;
    private String managerFeedback;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LocalDate getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(LocalDate reviewDate) {
        this.reviewDate = reviewDate;
    }

    public Integer getTechnicalRating() {
        return technicalRating;
    }

    public void setTechnicalRating(Integer technicalRating) {
        this.technicalRating = technicalRating;
    }

    public Integer getSoftSkillsRating() {
        return softSkillsRating;
    }

    public void setSoftSkillsRating(Integer softSkillsRating) {
        this.softSkillsRating = softSkillsRating;
    }

    public Integer getAttendanceRating() {
        return attendanceRating;
    }

    public void setAttendanceRating(Integer attendanceRating) {
        this.attendanceRating = attendanceRating;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public String getManagerFeedback() {
        return managerFeedback;
    }

    public void setManagerFeedback(String managerFeedback) {
        this.managerFeedback = managerFeedback;
    }
}