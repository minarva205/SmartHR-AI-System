package com.smarthr.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "expense_claims")
public class ExpenseClaim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;
    private String employeeName;
    private Double amount;
    private String category; // TRAVEL, MEALS, SOFTWARE, OFFICE_SUPPLIES, HEALTHCARE
    private String description;
    private String status; // PENDING, APPROVED, REJECTED
    private LocalDate submittedDate;
    
    @Column(length = 500)
    private String receiptUrl;

    // Simulated OCR Data
    private String ocrMerchant;
    private Double ocrAmount;
    private LocalDate ocrDate;
    private Double ocrConfidence; // e.g., 98.4

    // Simulated AI Auditor Assessment
    private String aiAuditStatus; // AUTO_APPROVED, FLAGGED_HIGH_RISK, MANUAL_REVIEW
    @Column(length = 1000)
    private String aiAuditReason;

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

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(LocalDate submittedDate) {
        this.submittedDate = submittedDate;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }

    public String getOcrMerchant() {
        return ocrMerchant;
    }

    public void setOcrMerchant(String ocrMerchant) {
        this.ocrMerchant = ocrMerchant;
    }

    public Double getOcrAmount() {
        return ocrAmount;
    }

    public void setOcrAmount(Double ocrAmount) {
        this.ocrAmount = ocrAmount;
    }

    public LocalDate getOcrDate() {
        return ocrDate;
    }

    public void setOcrDate(LocalDate ocrDate) {
        this.ocrDate = ocrDate;
    }

    public Double getOcrConfidence() {
        return ocrConfidence;
    }

    public void setOcrConfidence(Double ocrConfidence) {
        this.ocrConfidence = ocrConfidence;
    }

    public String getAiAuditStatus() {
        return aiAuditStatus;
    }

    public void setAiAuditStatus(String aiAuditStatus) {
        this.aiAuditStatus = aiAuditStatus;
    }

    public String getAiAuditReason() {
        return aiAuditReason;
    }

    public void setAiAuditReason(String aiAuditReason) {
        this.aiAuditReason = aiAuditReason;
    }
}
