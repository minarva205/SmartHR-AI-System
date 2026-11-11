package com.smarthr.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "churn_records")
public class ChurnRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", unique = true)
    private Long employeeId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "risk_score")
    private Double riskScore; // 0 to 100

    @Column(name = "risk_level")
    private String riskLevel; // "LOW", "MEDIUM", "HIGH"

    @Column(name = "risk_factors")
    private String riskFactors; // comma-separated list of factors

    @Column(name = "satisfaction_score")
    private Double satisfactionScore; // 1.0 to 5.0

    @Column(name = "work_life_balance")
    private Double workLifeBalance; // 1.0 to 5.0

    @Column(name = "stress_level")
    private Double stressLevel; // 1.0 to 5.0

    @Column(name = "growth_opportunity")
    private Double growthOpportunity; // 1.0 to 5.0

    @Column(name = "ai_recommendation", length = 2000)
    private String aiRecommendation;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRiskFactors() {
        return riskFactors;
    }

    public void setRiskFactors(String riskFactors) {
        this.riskFactors = riskFactors;
    }

    public Double getSatisfactionScore() {
        return satisfactionScore;
    }

    public void setSatisfactionScore(Double satisfactionScore) {
        this.satisfactionScore = satisfactionScore;
    }

    public Double getWorkLifeBalance() {
        return workLifeBalance;
    }

    public void setWorkLifeBalance(Double workLifeBalance) {
        this.workLifeBalance = workLifeBalance;
    }

    public Double getStressLevel() {
        return stressLevel;
    }

    public void setStressLevel(Double stressLevel) {
        this.stressLevel = stressLevel;
    }

    public Double getGrowthOpportunity() {
        return growthOpportunity;
    }

    public void setGrowthOpportunity(Double growthOpportunity) {
        this.growthOpportunity = growthOpportunity;
    }

    public String getAiRecommendation() {
        return aiRecommendation;
    }

    public void setAiRecommendation(String aiRecommendation) {
        this.aiRecommendation = aiRecommendation;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
